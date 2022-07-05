package com.power4j.fist.boot.hibernate;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/7/5
 * @since 1.0
 */
@Entity
public class Account {
	@Id
	@GeneratedValue
	Long id;

	@TenantId
	String tenantId;
}
