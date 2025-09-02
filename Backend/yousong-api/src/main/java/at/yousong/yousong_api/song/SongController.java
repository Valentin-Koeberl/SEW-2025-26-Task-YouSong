package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.artist.ArtistRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "*")
@Validated // <<-- WICHTIG, damit @NotBlank/@Min auf Parametern greift
public class SongController {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;

    public SongController(SongRepository songRepository, ArtistRepository artistRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
    }

    // Liste mit Pagination (Default 5)
    @GetMapping
    public ResponseEntity<Page<Song>> getAllSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Song> songs = songRepository.findAll(pageable);
        return ResponseEntity.ok(songs);
    }

    // Einzelner Song (für Edit) – mit Parametervalidierung
    @GetMapping("/{id}")
    public ResponseEntity<Song> getSongById(@PathVariable @Min(1) Long id) {
        return songRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Anlegen – übernimmt Artist per { "artist": { "id": ... } }
    @PostMapping
    public ResponseEntity<Song> createSong(@Valid @RequestBody Song newSong) {
        Artist artist = artistRepository.findById(newSong.getArtist().getId())
                .orElseThrow(() -> new RuntimeException("Artist not found"));
        newSong.setArtist(artist);
        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSong);
    }

    // Aktualisieren
    @PutMapping("/{id}")
    public ResponseEntity<Song> updateSong(@PathVariable @Min(1) Long id, @Valid @RequestBody Song updatedSong) {
        return songRepository.findById(id)
                .map(song -> {
                    song.setTitle(updatedSong.getTitle());
                    song.setGenre(updatedSong.getGenre());
                    song.setLength(updatedSong.getLength());

                    Artist artist = artistRepository.findById(updatedSong.getArtist().getId())
                            .orElseThrow(() -> new RuntimeException("Artist not found"));
                    song.setArtist(artist);

                    Song savedSong = songRepository.save(song);
                    return ResponseEntity.ok(savedSong);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable @Min(1) Long id) {
        if (songRepository.existsById(id)) {
            songRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Suche (ohne Pagination) – Param VALIDATION aktiv
    @GetMapping("/search")
    public ResponseEntity<List<Song>> searchSongs(
            @RequestParam
            @NotBlank(message = "Query must not be blank.")
            @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters.")
            String query) {

        List<Song> songs = songRepository
                .findByTitleContainingIgnoreCaseOrArtist_NameContainingIgnoreCase(query, query);

        if (songs.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(songs);
    }
}
