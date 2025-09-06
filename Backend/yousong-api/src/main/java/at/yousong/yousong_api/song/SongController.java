package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.artist.ArtistRepository;
import at.yousong.yousong_api.song.dto.ArtistRefDto;
import at.yousong.yousong_api.song.dto.SongDetailDto;
import at.yousong.yousong_api.user.Benutzer;
import at.yousong.yousong_api.user.BenutzerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "*")
@Validated
public class SongController {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final BenutzerRepository benutzerRepository;

    public SongController(SongRepository songRepository, ArtistRepository artistRepository, BenutzerRepository benutzerRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
        this.benutzerRepository = benutzerRepository;
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

    // ---- DETAIL (DTO; inkl. musicData & version) ----
    @GetMapping("/{id}")
    public ResponseEntity<SongDetailDto> getSongById(@PathVariable @Min(1) Long id) {
        return songRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private SongDetailDto toDto(Song s) {
        Artist a = s.getArtist();
        ArtistRefDto aDto = new ArtistRefDto(a.getId(), a.getName());
        return new SongDetailDto(
                s.getId(),
                s.getTitle(),
                s.getGenre(),
                s.getLength(),
                s.getVersion(),
                aDto,
                s.getMusicData()
        );
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

    // ---- CREATE (Owner setzen) ----
    @PostMapping
    public ResponseEntity<?> createSong(@Valid @RequestBody Song newSong) {
        Benutzer current = getCurrentUser();
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to create a song.");
        }
        Artist artist = artistRepository.findById(newSong.getArtist().getId())
                .orElseThrow(() -> new RuntimeException("Artist not found"));
        newSong.setArtist(artist);
        newSong.setOwner(current);
        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedSong));
    }

    // ---- UPDATE (nur Besitzer; Version prüfen) ----
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSong(@PathVariable @Min(1) Long id, @Valid @RequestBody Song updatedSong) {
        if (updatedSong.getVersion() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Version is required for updates.");
        }
        Benutzer current = getCurrentUser();
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to update a song.");
        }

        return songRepository.findById(id).map(existing -> {
            if (existing.getOwner() == null || !Objects.equals(existing.getOwner().getId(), current.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only edit your own songs.");
            }
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

            Song saved = songRepository.saveAndFlush(existing);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ---- DELETE (nur Besitzer) ----
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSong(@PathVariable @Min(1) Long id) {
        Benutzer current = getCurrentUser();
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to delete a song.");
        }
        Optional<Song> opt = songRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Song existing = opt.get();
        if (existing.getOwner() == null || !Objects.equals(existing.getOwner().getId(), current.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own songs.");
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

    private Benutzer getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        if (username == null || "anonymousUser".equals(username)) return null;
        return benutzerRepository.findByUsername(username).orElse(null);
    }
}
