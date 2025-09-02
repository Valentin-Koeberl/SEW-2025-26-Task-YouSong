package at.yousong.yousong_api.artist;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/artists")
@CrossOrigin(origins = "*")
public class ArtistController {

    private final ArtistRepository artistRepository;

    public ArtistController(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @GetMapping
    public ResponseEntity<List<Artist>> getAll() {
        return ResponseEntity.ok(artistRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Artist> getById(@PathVariable Long id) {
        Optional<Artist> artist = artistRepository.findById(id);
        return artist.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Artist> create(@Valid @RequestBody Artist artist) {
        if (artistRepository.existsByNameIgnoreCase(artist.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Artist saved = artistRepository.save(artist);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Artist> update(@PathVariable Long id, @Valid @RequestBody Artist artist) {
        Optional<Artist> existing = artistRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Artist updated = existing.get();
        updated.setName(artist.getName());
        updated.setDescription(artist.getDescription());
        artistRepository.save(updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!artistRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        artistRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
