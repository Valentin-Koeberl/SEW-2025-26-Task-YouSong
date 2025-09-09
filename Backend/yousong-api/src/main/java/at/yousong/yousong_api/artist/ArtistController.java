package at.yousong.yousong_api.artist;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

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
    public ResponseEntity<List<Artist>> getAllArtists() {
        return ResponseEntity.ok(artistRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Artist> getArtistById(@PathVariable Long id) {
        return artistRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createArtist(@Valid @RequestBody Artist artist) {
        if (artistRepository.existsByNameIgnoreCase(artist.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Artist with this name already exists!");
        }
        Artist savedArtist = artistRepository.save(artist);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedArtist);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateArtist(
            @PathVariable Long id,
            @Valid @RequestBody Artist artistDetails
    ) {
        Optional<Artist> existing = artistRepository.findById(id);

        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Artist artist = existing.get();
        artist.setName(artistDetails.getName());
        artist.setDescription(artistDetails.getDescription());

        Artist updatedArtist = artistRepository.save(artist);
        return ResponseEntity.ok(updatedArtist);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArtist(@PathVariable Long id) {
        if (!artistRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            artistRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Artist cannot be deleted because it is referenced by songs.");
        }
    }
}
