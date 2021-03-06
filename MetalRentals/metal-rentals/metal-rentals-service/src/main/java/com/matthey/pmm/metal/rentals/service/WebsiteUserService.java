package com.matthey.pmm.metal.rentals.service;

import com.matthey.pmm.metal.rentals.EndurConnector;
import com.matthey.pmm.metal.rentals.ImmutableWebsiteUser;
import com.matthey.pmm.metal.rentals.WebsiteUser;
import com.matthey.pmm.metal.rentals.document.EmailSender;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.MoreCollectors.onlyElement;
import static java.util.stream.Collectors.toSet;
import static org.passay.IllegalRegexRule.ERROR_CODE;

@Service
public class WebsiteUserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(WebsiteUserService.class);

    private final EndurConnector endurConnector;
    private final String apiUrl = "/users";
    private final PasswordEncoder passwordEncoder;

    public WebsiteUserService(EndurConnector endurConnector, PasswordEncoder passwordEncoder) {
        this.endurConnector = endurConnector;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            var user = getUser(username);
            return new User(user.userFullName(), user.encryptedPassword(), Set.of());
        } catch (Exception e) {
            throw new UsernameNotFoundException("cannot find user " + username, e);
        }
    }

    public void resetPassword(String username) {
        logger.info("reset password for user: {}", username);

        var selectedUser = getUser(username);
        var newPassword = genPassword();
        var
                updatedUser
                = ((ImmutableWebsiteUser) selectedUser).withEncryptedPassword(passwordEncoder.encode(newPassword));
        new EmailSender().send("Your New Password for Metal Rentals",
                               "This is your new password: " + newPassword,
                               null,
                               updatedUser.email());
        endurConnector.post(apiUrl, updatedUser);
    }

    public WebsiteUser getUser(String username) {
        return Stream.of(retrieveUserFromEndur())
                .filter(user -> user.userFullName().equals(username))
                .distinct()
                .collect(onlyElement());
    }

    private WebsiteUser[] retrieveUserFromEndur() {
        WebsiteUser[] users = endurConnector.get(apiUrl, WebsiteUser[].class);
        checkNotNull(users);
        return users;
    }

    public Set<SimplifiedWebsiteUser> getAllUsersForUI() {
        return Stream.of(retrieveUserFromEndur()).map(SimplifiedWebsiteUser::from).collect(toSet());
    }

    private String genPassword() {
        PasswordGenerator gen = new PasswordGenerator();
        CharacterData lowerCaseChars = EnglishCharacterData.LowerCase;
        CharacterRule lowerCaseRule = new CharacterRule(lowerCaseChars);
        lowerCaseRule.setNumberOfCharacters(2);

        CharacterData upperCaseChars = EnglishCharacterData.UpperCase;
        CharacterRule upperCaseRule = new CharacterRule(upperCaseChars);
        upperCaseRule.setNumberOfCharacters(2);

        CharacterData digitChars = EnglishCharacterData.Digit;
        CharacterRule digitRule = new CharacterRule(digitChars);
        digitRule.setNumberOfCharacters(2);

        CharacterData specialChars = new CharacterData() {
            public String getErrorCode() {
                return ERROR_CODE;
            }

            public String getCharacters() {
                return "!@#$%^&*()_+";
            }
        };
        CharacterRule splCharRule = new CharacterRule(specialChars);
        splCharRule.setNumberOfCharacters(2);

        return gen.generatePassword(10, splCharRule, lowerCaseRule, upperCaseRule, digitRule);
    }

}
