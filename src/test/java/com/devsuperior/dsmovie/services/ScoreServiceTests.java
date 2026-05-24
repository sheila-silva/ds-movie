package com.devsuperior.dsmovie.services;

import com.devsuperior.dsmovie.dto.MovieDTO;
import com.devsuperior.dsmovie.dto.ScoreDTO;
import com.devsuperior.dsmovie.entities.MovieEntity;
import com.devsuperior.dsmovie.entities.ScoreEntity;
import com.devsuperior.dsmovie.entities.UserEntity;
import com.devsuperior.dsmovie.repositories.MovieRepository;
import com.devsuperior.dsmovie.repositories.ScoreRepository;
import com.devsuperior.dsmovie.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dsmovie.tests.MovieFactory;
import com.devsuperior.dsmovie.tests.ScoreFactory;
import com.devsuperior.dsmovie.tests.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class ScoreServiceTests {

	@InjectMocks
	private ScoreService service;

	@Mock
	private UserService userService;

	@Mock
	private MovieRepository movieRepository;

	@Mock
	private ScoreRepository scoreRepository;

	private Long existingMovieId;
	private Long nonExistingMovieId;

	private UserEntity user;
	private MovieEntity movie;
	private ScoreEntity score;
	private ScoreDTO scoreDTO;

	@BeforeEach
	void setUp() {
		existingMovieId = 1L;
		nonExistingMovieId = 999L;

		user = UserFactory.createUserEntity();
		score = ScoreFactory.createScoreEntity();
		movie = score.getId().getMovie();
		scoreDTO = ScoreFactory.createScoreDTO();

		// authenticated user
		when(userService.authenticated()).thenReturn(user);

		// movie repository
		when(movieRepository.findById(existingMovieId))
				.thenReturn(Optional.of(movie));
		when(movieRepository.findById(nonExistingMovieId))
				.thenReturn(Optional.empty());

		// score repository – saveAndFlush returns the score entity
		when(scoreRepository.saveAndFlush(any(ScoreEntity.class)))
				.thenReturn(score);

		// movie repository save
		when(movieRepository.save(any(MovieEntity.class)))
				.thenReturn(movie);
	}

	@Test
	public void saveScoreShouldReturnMovieDTO() {
		MovieDTO result = service.saveScore(scoreDTO);

		assertNotNull(result);
		assertEquals(movie.getId(), result.getId());
		verify(userService).authenticated();
		verify(movieRepository).findById(existingMovieId);
		verify(scoreRepository).saveAndFlush(any(ScoreEntity.class));
		verify(movieRepository).save(any(MovieEntity.class));
	}

	@Test
	public void saveScoreShouldThrowResourceNotFoundExceptionWhenNonExistingMovieId() {
		ScoreDTO invalidDTO = new ScoreDTO(nonExistingMovieId, 4.5);

		assertThrows(ResourceNotFoundException.class,
				() -> service.saveScore(invalidDTO));

		verify(userService).authenticated();
		verify(movieRepository).findById(nonExistingMovieId);
		verify(scoreRepository, never()).saveAndFlush(any());
	}
}