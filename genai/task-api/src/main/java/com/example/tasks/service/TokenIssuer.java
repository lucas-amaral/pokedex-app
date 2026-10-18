package com.example.tasks.service;

import com.example.tasks.entity.User;

/** Output port: signs access tokens. */
public interface TokenIssuer {

    AccessToken issue(User user);
}
