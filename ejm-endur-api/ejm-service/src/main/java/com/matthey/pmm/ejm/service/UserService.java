package com.matthey.pmm.ejm.service;

import com.matthey.pmm.ejm.EndurConnector;
import com.matthey.pmm.ejm.ServiceUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.MoreCollectors.onlyElement;

@Service
public class UserService implements UserDetailsService {

    private final EndurConnector endurConnector;

    public UserService(EndurConnector endurConnector) {
        this.endurConnector = endurConnector;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            var user = getUser(username);
            return new User(user.username(), user.encryptedPassword(), Set.of());
        } catch (Exception e) {
            throw new UsernameNotFoundException("cannot find user " + username, e);
        }
    }

    public ServiceUser getUser(String username) {
        return Stream.of(retrieveUserFromEndur())
                .filter(user -> user.username().equals(username))
                .distinct()
                .collect(onlyElement());
    }

    private ServiceUser[] retrieveUserFromEndur() {
        String apiUrl = "/users";
        return endurConnector.get(apiUrl, ServiceUser[].class);
    }
}
