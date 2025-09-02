package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.artist.ArtistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "*")
public class SongController {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;

    public SongController(SongRepository songRepository, ArtistRepository artistRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
    }

    // ===== DTOs =====
    public record SongDto(Long id, String title, String genre, int length, Long artistId, String artistName) {}
    public static class SongRequest {
        public String title;
        public String genre;
        public int length;
        public Long artistId;
    }

    private SongDto toDto(Song s) {
        return new SongDto(
                s.getId(),
                s.getTitle(),
                s.getGenre(),
                s.getLength(),
                s.getArtist() != null ? s.getArtist().getId() : null,
                s.getArtist() != null ? s.getArtist().getName() : null
        );
    }

    public record SongPageDto(List<SongDto> songs, int page, int totalPages) {}

    // ===== READ =====

    @GetMapping
    public ResponseEntity<SongPageDto> getAll(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "5") int size) {
        Page<Song> songPage = songRepository.findAll(PageRequest.of(page, size));
        List<SongDto> list = songPage.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(new SongPageDto(list, songPage.getNumber(), songPage.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongDto> getById(@PathVariable Long id) {
        return songRepository.findById(id)
                .map(s -> ResponseEntity.ok(toDto(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SongDto>> search(@RequestParam String query) {
        List<SongDto> list = songRepository
                .findByTitleContainingIgnoreCaseOrArtist_NameContainingIgnoreCase(query, query)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    // ===== CREATE =====

    @PostMapping
    public ResponseEntity<SongDto> create(@RequestBody SongRequest req) {
        if (req.title == null || req.title.isBlank() || req.artistId == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Artist> artistOpt = artistRepository.findById(req.artistId);
        if (artistOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
        Song song = new Song(null, req.title, req.genre, req.length, artistOpt.get());
        Song saved = songRepository.save(song);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    // ===== UPDATE =====

    @PutMapping("/{id}")
    public ResponseEntity<SongDto> update(@PathVariable Long id, @RequestBody SongRequest req) {
        Optional<Song> existingOpt = songRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Song song = existingOpt.get();
        if (req.title != null) song.setTitle(req.title);
        song.setGenre(req.genre);
        song.setLength(req.length);

        if (req.artistId != null) {
            Optional<Artist> artistOpt = artistRepository.findById(req.artistId);
            if (artistOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
            }
            song.setArtist(artistOpt.get());
        }
        Song saved = songRepository.save(song);
        return ResponseEntity.ok(toDto(saved));
    }

    // ===== DELETE =====

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!songRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        songRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
