package com.power4j.fist3.examples.webdatasecurity.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import com.power4j.fist.boot.security.core.UserInfo;
import com.power4j.fist.boot.security.core.UserInfoSupplier;
import com.power4j.fist.boot.web.constant.HttpConstant;
import com.power4j.fist.data.crud.validate.ObjectValidator;
import com.power4j.fist.data.domain.Pageable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProfileController {

	private final JdbcTemplate jdbcTemplate;

	private final ObjectValidator objectValidator;

	private final UserInfoSupplier userInfoSupplier;

	ProfileController(JdbcTemplate jdbcTemplate, ObjectValidator objectValidator, UserInfoSupplier userInfoSupplier) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectValidator = objectValidator;
		this.userInfoSupplier = userInfoSupplier;
	}

	@GetMapping("/api/profiles/{id}")
	Map<String, Object> getProfile(@PathVariable long id, Pageable pageable, HttpServletRequest request) {
		String username = this.jdbcTemplate.queryForObject("select username from example_profile where id = ?",
				String.class, id);
		UserInfo userInfo = this.userInfoSupplier.get();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", id);
		body.put("username", username);
		body.put("pageNumber", pageable.getPageNumber());
		body.put("pageSize", pageable.getPageSize());
		body.put("requestId", request.getHeader(HttpConstant.Header.KEY_REQUEST_ID));
		body.put("currentUser", userInfo == null ? null : userInfo.getUsername());
		return body;
	}

	@PostMapping("/api/profiles")
	Map<String, Object> createProfile(@RequestBody ProfileCreateRequest request) {
		this.objectValidator.validateAll(request);
		this.jdbcTemplate.update("insert into example_profile(id, username) values(?, ?)", 2L, request.getUsername());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", 2L);
		body.put("username", request.getUsername());
		return body;
	}

}
