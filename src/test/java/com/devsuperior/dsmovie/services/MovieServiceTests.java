package com.devsuperior.dsmovie.services;

import com.devsuperior.dsmovie.dto.MovieDTO;
import com.devsuperior.dsmovie.entities.MovieEntity;
import com.devsuperior.dsmovie.repositories.MovieRepository;
import com.devsuperior.dsmovie.services.exceptions.DatabaseException;
import com.devsuperior.dsmovie.services.exceptions.ResourceNotFoundException;
import com.devsuperior.dsmovie.tests.MovieFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class MovieServiceTests {

	@InjectMocks
	private MovieService service;

	@Mock
	private MovieRepository repository;

	private Long existingId;
	private Long nonExistingId;
	private Long dependentId;

	private MovieEntity movie;
	private MovieDTO movieDTO;
	private PageImpl<MovieEntity> page;

	@BeforeEach
	void setUp() {
		existingId = 1L;
		nonExistingId = 999L;
		dependentId = 2L;

		movie = MovieFactory.createMovieEntity();
		movieDTO = MovieFactory.createMovieDTO();
		page = new PageImpl<>(List.of(movie));

		// findAll
		when(repository.searchByTitle(any(), any(Pageable.class)))
				.thenReturn(page);

		// findById
		when(repository.findById(existingId))
				.thenReturn(Optional.of(movie));
		when(repository.findById(nonExistingId))
				.thenReturn(Optional.empty());

		// save (insert)
		when(repository.save(any(MovieEntity.class)))
				.thenReturn(movie);

		// getReferenceById (update)
		when(repository.getReferenceById(existingId))
				.thenReturn(movie);
		when(repository.getReferenceById(nonExistingId))
				.thenThrow(EntityNotFoundException.class);

		// delete
		when(repository.existsById(existingId))
				.thenReturn(true);
		when(repository.existsById(nonExistingId))
				.thenReturn(false);
		when(repository.existsById(dependentId))
				.thenReturn(true);

		doNothing().when(repository).deleteById(existingId);
		doThrow(DataIntegrityViolationException.class)
				.when(repository).deleteById(dependentId);
	}

	@Test
	public void findAllShouldReturnPagedMovieDTO() {
		Pageable pageable = PageRequest.of(0, 12);

		Page<MovieDTO> result = service.findAll("", pageable);

		assertNotNull(result);
		assertEquals(1, result.getTotalElements());
		assertEquals(movie.getTitle(), result.getContent().get(0).getTitle());
		verify(repository).searchByTitle(eq(""), eq(pageable));
	}

	@Test
	public void findByIdShouldReturnMovieDTOWhenIdExists() {
		MovieDTO result = service.findById(existingId);

		assertNotNull(result);
		assertEquals(existingId, result.getId());
		assertEquals(movie.getTitle(), result.getTitle());
		verify(repository).findById(existingId);
	}

	@Test
	public void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
		assertThrows(ResourceNotFoundException.class,
				() -> service.findById(nonExistingId));
		verify(repository).findById(nonExistingId);
	}

	@Test
	public void insertShouldReturnMovieDTO() {
		MovieDTO result = service.insert(movieDTO);

		assertNotNull(result);
		assertEquals(movie.getTitle(), result.getTitle());
		verify(repository).save(any(MovieEntity.class));
	}

	@Test
	public void updateShouldReturnMovieDTOWhenIdExists() {
		MovieDTO result = service.update(existingId, movieDTO);

		assertNotNull(result);
		assertEquals(existingId, result.getId());
		verify(repository).getReferenceById(existingId);
		verify(repository).save(any(MovieEntity.class));
	}

	@Test
	public void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
		assertThrows(ResourceNotFoundException.class,
				() -> service.update(nonExistingId, movieDTO));
		verify(repository).getReferenceById(nonExistingId);
	}

	@Test
	public void deleteShouldDoNothingWhenIdExists() {
		assertDoesNotThrow(() -> service.delete(existingId));
		verify(repository).existsById(existingId);
		verify(repository).deleteById(existingId);
	}

	@Test
	public void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {
		assertThrows(ResourceNotFoundException.class,
				() -> service.delete(nonExistingId));
		verify(repository).existsById(nonExistingId);
		verify(repository, never()).deleteById(nonExistingId);
	}

	@Test
	public void deleteShouldThrowDatabaseExceptionWhenDependentId() {
		assertThrows(DatabaseException.class,
				() -> service.delete(dependentId));
		verify(repository).existsById(dependentId);
		verify(repository).deleteById(dependentId);
	}
}