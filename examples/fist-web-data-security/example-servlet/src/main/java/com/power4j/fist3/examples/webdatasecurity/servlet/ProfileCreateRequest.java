package com.power4j.fist3.examples.webdatasecurity.servlet;

import jakarta.validation.constraints.NotBlank;

class ProfileCreateRequest {

	@NotBlank
	private String username;

	String getUsername() {
		return this.username;
	}

	void setUsername(String username) {
		this.username = username;
	}

}
