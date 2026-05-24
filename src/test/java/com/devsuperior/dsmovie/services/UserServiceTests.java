package com.devsuperior.dsmovie.services;

import com.devsuperior.dsmovie.entities.UserEntity;
import com.devsuperior.dsmovie.projections.UserDetailsProjection;
import com.devsuperior.dsmovie.repositories.UserRepository;
import com.devsuperior.dsmovie.tests.UserDetailsFactory;
import com.devsuperior.dsmovie.tests.UserFactory;
import com.devsuperior.dsmovie.utils.CustomUserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class UserServiceTests {

	@InjectMocks
	private UserService service;

	@Mock
	private UserRepository repository;

	@Mock
	private CustomUserUtil userUtil;

	private String existingUsername;
	private String nonExistingUsername;

	private UserEntity user;
	private List<UserDetailsProjection> userDetailsProjections;

	@BeforeEach
	void setUp() {
		existingUsername = "maria@gmail.com";
		nonExistingUsername = "unknown@gmail.com";

		user = UserFactory.createUserEntity();
		userDetailsProjections = UserDetailsFactory.createCustomAdminClientUser(existingUsername);

		// authenticated() – CustomUserUtil
		when(userUtil.getLoggedUsername()).thenReturn(existingUsername);

		// repository lookups
		when(repository.findByUsername(existingUsername))
				.thenReturn(Optional.of(user));
		when(repository.findByUsername(nonExistingUsername))
				.thenReturn(Optional.empty());

		when(repository.searchUserAndRolesByUsername(existingUsername))
				.thenReturn(userDetailsProjections);
		when(repository.searchUserAndRolesByUsername(nonExistingUsername))
				.thenReturn(List.of());
	}

	@Test
	public void authenticatedShouldReturnUserEntityWhenUserExists() {
		UserEntity result = service.authenticated();

		assertNotNull(result);
		assertEquals(existingUsername, result.getUsername());
		verify(userUtil).getLoggedUsername();
		verify(repository).findByUsername(existingUsername);
	}

	@Test
	public void authenticatedShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExists() {
		// Make getLoggedUsername throw so authenticated() falls into the catch block
		when(userUtil.getLoggedUsername()).thenThrow(RuntimeException.class);

		assertThrows(UsernameNotFoundException.class,
				() -> service.authenticated());
	}

	@Test
	public void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
		UserDetails result = service.loadUserByUsername(existingUsername);

		assertNotNull(result);
		assertEquals(existingUsername, result.getUsername());
		// Should contain both roles loaded from the projection list
		assertEquals(2, result.getAuthorities().size());
		verify(repository).searchUserAndRolesByUsername(existingUsername);
	}

	@Test
	public void loadUserByUsernameShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExists() {
		assertThrows(UsernameNotFoundException.class,
				() -> service.loadUserByUsername(nonExistingUsername));
		verify(repository).searchUserAndRolesByUsername(nonExistingUsername);
	}
}