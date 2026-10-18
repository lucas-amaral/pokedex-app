package com.example.tasks.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;

/** 401 and 403 are decided by security filters before any controller runs, so they are written here, in RFC 9457 shape. */
final class ProblemResponses {

    private ProblemResponses() {
    }

    static void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String title,
                      String detail, JsonMapper json) throws IOException {
        var problem = new LinkedHashMap<String, Object>();
        problem.put("title", title);
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", request.getRequestURI());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getOutputStream().write(json.writeValueAsBytes(problem));
    }
}
