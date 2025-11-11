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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.Base64;

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

    // 🔹 Alle Songs mit Paging
    @GetMapping
    public ResponseEntity<Page<SongProjection>> getAllSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(songRepository.findAllProjectedBy(pageable));
    }

    // 🔹 Katalog mit optionaler Suche (komplett Repository-basiert)
    @GetMapping("/catalog")
    public ResponseEntity<Page<SongProjection>> catalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String q) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        String query = (q != null && q.trim().length() >= 2) ? q.trim() : null;
        return ResponseEntity.ok(songRepository.catalogNoGenres(query, pageable));
    }

    // 🔹 Direkte Suche ohne Paging (rein über Repository)
    @GetMapping("/search")
    public ResponseEntity<List<SongProjection>> searchSongs(
            @RequestParam
            @NotBlank(message = "Query must not be blank.")
            @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters.")
            String query) {
        return ResponseEntity.ok(songRepository.searchProjected(query));
    }

    // 🔹 Song-Detailansicht
    @GetMapping("/{id}")
    public ResponseEntity<SongDetailDto> getSongById(@PathVariable @Min(1) Long id) {
        return songRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Song-Audio abrufen
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
                        return ResponseEntity.internalServerError()
                                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                                .body(new byte[0]);
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .body(new byte[0]));
    }

    // 🔹 Song erstellen
    @PostMapping
    public ResponseEntity<?> createSong(@Valid @RequestBody Song newSong) {
        Benutzer current = getCurrentUser();
        if (current == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to create a song.");

        Long artistId = Optional.ofNullable(newSong.getArtist()).map(Artist::getId).orElse(null);
        if (artistId == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Artist is required."));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist not found"));

        newSong.setArtist(artist);
        newSong.setOwner(current);

        Song saved = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    // 🔹 Song aktualisieren
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSong(@PathVariable @Min(1) Long id, @Valid @RequestBody Song updatedSong) {
        Benutzer current = getCurrentUser();
        if (current == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to update a song.");

        if (updatedSong.getVersion() == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Version is required."));

        return songRepository.findById(id).map(existing -> {
            if (!Objects.equals(existing.getOwner().getId(), current.getId()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only edit your own songs.");

            if (!Objects.equals(existing.getVersion(), updatedSong.getVersion()))
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Dieser Song wurde inzwischen geändert. Bitte lade die Seite neu.");

            Long artistId = Optional.ofNullable(updatedSong.getArtist()).map(Artist::getId).orElse(null);
            if (artistId == null)
                return ResponseEntity.badRequest().body(Map.of("message", "Artist is required."));

            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new RuntimeException("Artist not found"));

            existing.setTitle(updatedSong.getTitle());
            existing.setLength(updatedSong.getLength());
            existing.setMusicData(updatedSong.getMusicData());
            existing.setArtist(artist);

            Song saved = songRepository.saveAndFlush(existing);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Song löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSong(@PathVariable @Min(1) Long id) {
        Benutzer current = getCurrentUser();
        if (current == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to delete a song.");

        return songRepository.findById(id).map(song -> {
            if (!Objects.equals(song.getOwner().getId(), current.getId()))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own songs.");

            songRepository.delete(song);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Mapper Song → DTO
    private SongDetailDto toDto(Song s) {
        Artist a = s.getArtist();
        ArtistRefDto aDto = new ArtistRefDto(a.getId(), a.getName());
        return new SongDetailDto(
                s.getId(),
                s.getTitle(),
                s.getGenres(),
                s.getLength(),
                s.getVersion(),
                aDto,
                s.getMusicData()
        );
    }

    // 🔹 Aktueller Benutzer aus SecurityContext
    private Benutzer getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        if (username == null || "anonymousUser".equals(username)) return null;
        return benutzerRepository.findByUsername(username).orElse(null);
    }
}
