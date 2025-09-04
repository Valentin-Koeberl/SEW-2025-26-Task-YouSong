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
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "*")
@Validated
public class SongController {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;

    public SongController(SongRepository songRepository, ArtistRepository artistRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Song>> getAllSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return ResponseEntity.ok(songRepository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Song> getSongById(@PathVariable @Min(1) Long id) {
        return songRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Song> createSong(@Valid @RequestBody Song newSong) {
        Artist artist = artistRepository.findById(newSong.getArtist().getId())
                .orElseThrow(() -> new RuntimeException("Artist not found"));
        newSong.setArtist(artist);
        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSong);
    }

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
                    return ResponseEntity.ok(songRepository.save(song));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable @Min(1) Long id) {
        if (!songRepository.existsById(id)) return ResponseEntity.notFound().build();
        songRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Song>> searchSongs(
            @RequestParam
            @NotBlank(message = "Query must not be blank.")
            @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters.")
            String query) {
        return ResponseEntity.ok(
                songRepository.findByTitleContainingIgnoreCaseOrArtist_NameContainingIgnoreCase(query, query)
        );
    }
}
