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
import java.util.Objects;

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

    // ---- LISTE (nur Metadaten, ohne musicData) ----
    @GetMapping
    public ResponseEntity<Page<SongProjection>> getAllSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<SongProjection> songs = songRepository.findAllProjectedBy(pageable);
        return ResponseEntity.ok(songs);
    }

    // ---- DETAIL (inkl. musicData & version – für Editor) ----
    @GetMapping("/{id}")
    public ResponseEntity<Song> getSongById(@PathVariable @Min(1) Long id) {
        return songRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- STREAM (liefert nur Bytes; CORS + Content-Type korrekt) ----
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

    // ---- CREATE ----
    @PostMapping
    public ResponseEntity<Song> createSong(@Valid @RequestBody Song newSong) {
        Artist artist = artistRepository.findById(newSong.getArtist().getId())
                .orElseThrow(() -> new RuntimeException("Artist not found"));
        newSong.setArtist(artist);
        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSong);
    }

    // ---- UPDATE (strikte Versionsprüfung -> 409 bei Mismatch) ----
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSong(@PathVariable @Min(1) Long id, @Valid @RequestBody Song updatedSong) {
        if (updatedSong.getVersion() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Version is required for updates.");
        }

        return songRepository.findById(id)
                .map(existing -> {
                    if (!Objects.equals(existing.getVersion(), updatedSong.getVersion())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body("Dieser Song wurde inzwischen geändert. Bitte lade die Seite neu.");
                    }

                    existing.setTitle(updatedSong.getTitle());
                    existing.setGenre(updatedSong.getGenre());
                    existing.setLength(updatedSong.getLength());
                    existing.setMusicData(updatedSong.getMusicData());

                    Artist artist = artistRepository.findById(updatedSong.getArtist().getId())
                            .orElseThrow(() -> new RuntimeException("Artist not found"));
                    existing.setArtist(artist);

                    // WICHTIG: Version NICHT manuell setzen – JPA erhöht sie selbst
                    Song saved = songRepository.saveAndFlush(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- DELETE ----
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable @Min(1) Long id) {
        if (!songRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        songRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- SEARCH (nur Metadaten) ----
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
