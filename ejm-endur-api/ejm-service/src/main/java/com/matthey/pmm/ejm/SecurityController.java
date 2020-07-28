package com.matthey.pmm.ejm;

import com.auth0.jwt.JWT;
import com.matthey.pmm.ejm.service.JWTSecurity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static com.matthey.pmm.ejm.service.EJMService.API_PREFIX;

@SuppressWarnings("deprecation")
@Api(tags = {"Security"}, description = "APIs for authentication")
@RestController
@RequestMapping(API_PREFIX)
public class SecurityController {

    private final JWTSecurity jwtSecurity;

    public SecurityController(JWTSecurity jwtSecurity) {
        this.jwtSecurity = jwtSecurity;
    }

    @ApiOperation("retrieve a JWT given the correct credentials are provided")
    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        try {
            var token = new UsernamePasswordAuthenticationToken(username, password, Set.of());
            jwtSecurity.authenticationManager.authenticate(token);
            var jwt = JWT.create()
                    .withSubject(username)
                    .withExpiresAt(Date.from(LocalDateTime.now().plusDays(7).toInstant(ZoneOffset.UTC)))
                    .sign(HMAC512(jwtSecurity.secret.getBytes()));
            return new ResponseEntity<>(jwt, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid username/password", HttpStatus.UNAUTHORIZED);
        }
    }
}
