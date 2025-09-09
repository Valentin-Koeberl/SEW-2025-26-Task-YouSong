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
import org.springframework.util.MultiValueMap;
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

    @GetMapping
    public ResponseEntity<Page<SongProjection>> getAllSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<SongProjection> songs = songRepository.findAllProjectedBy(pageable);
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/catalog")
    public ResponseEntity<Page<SongProjection>> catalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String q,
            @RequestParam MultiValueMap<String, String> params) {

        // Query normalisieren (nur bei >=2 Zeichen wird gesucht)
        String query = (q == null || q.trim().length() < 2) ? null : q.trim();

        // Genres akzeptieren in allen üblichen Formen:
        // ?genres=Rock&genres=Pop  |  ?genres[]=Rock&genres[]=Pop  |  ?genresCsv=Rock,Pop  |  ?genres=Rock,Pop
        List<String> raw = new ArrayList<>();
        String csv = params.getFirst("genresCsv");
        if (csv != null) raw.add(csv);
        raw.addAll(params.getOrDefault("genres", List.of()));
        raw.addAll(params.getOrDefault("genres[]", List.of()));

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String token : raw) {
            if (token == null) continue;
            for (String s : token.split(",")) {
                String t = s == null ? "" : s.trim();
                if (!t.isEmpty()) set.add(t.toLowerCase());
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

        if (set.isEmpty()) {
            Page<SongProjection> res = songRepository.catalogNoGenres(query, pageable);
            return ResponseEntity.ok(res);
        } else {
            List<String> genres = new ArrayList<>(set);
            Page<SongProjection> res = songRepository.catalogWithGenres(query, genres, genres.size(), pageable);
            return ResponseEntity.ok(res);
        }
    }

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
                s.getGenres(),
                s.getLength(),
                s.getVersion(),
                aDto,
                s.getMusicData()
        );
    }

    // ---- STREAM ----
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

    @PostMapping
    public ResponseEntity<?> createSong(@Valid @RequestBody Song newSong) {
        Benutzer current = getCurrentUser();
        if (current == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login to create a song.");
        }

        Long artistId = Optional.ofNullable(newSong.getArtist()).map(Artist::getId).orElse(null);
        if (artistId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Artist is required."));
        }
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist not found"));

        List<String> normGenres = normalizeGenres(newSong.getGenres());
        if (normGenres.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Please provide at least one valid genre (1–80 chars)."));
        }

        newSong.setArtist(artist);
        newSong.setOwner(current);
        newSong.setGenres(normGenres);

        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(savedSong));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSong(@PathVariable @Min(1) Long id, @Valid @RequestBody Song updatedSong) {
        if (updatedSong.getVersion() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message","Version is required for updates."));
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
            existing.setLength(updatedSong.getLength());
            existing.setMusicData(updatedSong.getMusicData());

            List<String> normGenres = normalizeGenres(updatedSong.getGenres());
            if (normGenres.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Please provide at least one valid genre (1–80 chars)."));
            }
            existing.setGenres(normGenres);

            Long aId = Optional.ofNullable(updatedSong.getArtist()).map(Artist::getId).orElse(null);
            if (aId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Artist is required."));
            }
            Artist artist = artistRepository.findById(aId)
                    .orElseThrow(() -> new RuntimeException("Artist not found"));
            existing.setArtist(artist);

            Song saved = songRepository.saveAndFlush(existing);
            return ResponseEntity.ok(toDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

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

    @GetMapping("/search")
    public ResponseEntity<List<SongProjection>> searchSongs(
            @RequestParam
            @NotBlank(message = "Query must not be blank.")
            @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters.")
            String query) {
        return ResponseEntity.ok(songRepository.searchProjected(query));
    }

    private Benutzer getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        if (username == null || "anonymousUser".equals(username)) return null;
        return benutzerRepository.findByUsername(username).orElse(null);
    }

    private List<String> normalizeGenres(List<String> in) {
        if (in == null) return List.of();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String g : in) {
            if (g == null) continue;
            String t = g.trim();
            if (t.isEmpty()) continue;
            if (t.length() > 80) t = t.substring(0, 80);
            set.add(t);
        }
        return new ArrayList<>(set);
    }
}
