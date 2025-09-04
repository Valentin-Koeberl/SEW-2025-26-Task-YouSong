package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.artist.ArtistRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
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

    // ✅ Alle Songs (ohne Musikdaten)
    @GetMapping
    public ResponseEntity<Page<SongProjection>> getAllSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<SongProjection> songs = songRepository.findAllProjectedBy(pageable);
        return ResponseEntity.ok(songs);
    }

    // ✅ Einzelner Song inkl. Musikdaten (für Edit)
    @GetMapping("/{id}")
    public ResponseEntity<Song> getSongById(@PathVariable @Min(1) Long id) {
        return songRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Musik-Streaming
    @GetMapping("/{id}/music")
    public ResponseEntity<byte[]> getSongMusic(@PathVariable Long id) {
        return songRepository.findById(id)
                .filter(song -> song.getMusicData() != null && song.getMusicData().startsWith("data:audio"))
                .map(song -> {
                    try {
                        String base64Data = song.getMusicData().split(",")[1];
                        byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                        String contentType = song.getMusicData().substring(5, song.getMusicData().indexOf(";"));

                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_TYPE, contentType)
                                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                                .body(audioBytes);
                    } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                                .body(new byte[0]);
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .body(new byte[0]));
    }

    // ✅ Song erstellen
    @PostMapping
    public ResponseEntity<Song> createSong(@Valid @RequestBody Song newSong) {
        Artist artist = artistRepository.findById(newSong.getArtist().getId())
                .orElseThrow(() -> new RuntimeException("Artist not found"));
        newSong.setArtist(artist);
        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSong);
    }

    // ✅ Song aktualisieren
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
                    song.setMusicData(updatedSong.getMusicData());
                    return ResponseEntity.ok(songRepository.save(song));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Song löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable @Min(1) Long id) {
        if (!songRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        songRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Suche nach Songs (ohne Musikdaten)
    @GetMapping("/search")
    public ResponseEntity<List<SongProjection>> searchSongs(
            @RequestParam
            @NotBlank(message = "Query must not be blank.")
            @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters.")
            String query) {
        return ResponseEntity.ok(
                songRepository.findByTitleContainingIgnoreCaseOrArtist_NameContainingIgnoreCase(query, query)
        );
    }
}
