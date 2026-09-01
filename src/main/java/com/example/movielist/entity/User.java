package com.example.movielist.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A registered account. Table is named "users" (not "user") because "user" is
 * a reserved word in many SQL databases.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by Hibernate; not for app code to use directly
public class User extends BaseEntity {

	@Column(nullable = false, unique = true)
	private String email;

	/** BCrypt hash — never the raw password, never logged. */
	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = false)
	private String displayName;

	@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<FavoriteList> favoriteLists = new ArrayList<>();

	/** Creates a new user record with an already-hashed password. */
	public User(String email, String passwordHash, String displayName) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
	}
}
