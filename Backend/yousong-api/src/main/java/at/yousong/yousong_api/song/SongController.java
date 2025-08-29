package at.yousong.yousong_api.song;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "*")
public class SongController {

    private final SongRepository songRepository;

    public SongController(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    // GET - Alle Songs anzeigen
    @GetMapping
    public List<Song> getAllSongs() {
        return songRepository.findAll();
    }

    // POST - Neuen Song speichern
    @PostMapping
    public ResponseEntity<Song> createSong(@RequestBody Song newSong) {
        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSong);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Song> updateSong(@PathVariable Long id, @RequestBody Song updatedSong) {
        return songRepository.findById(id)
                .map(song -> {
                    song.setTitle(updatedSong.getTitle());
                    song.setArtist(updatedSong.getArtist());
                    song.setGenre(updatedSong.getGenre());
                    song.setLength(updatedSong.getLength());
                    Song savedSong = songRepository.save(song);
                    return ResponseEntity.ok(savedSong);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
        if (songRepository.existsById(id)) {
            songRepository.deleteById(id);
            return ResponseEntity.noContent().build(); // HTTP 204 → Erfolgreich gelöscht
        } else {
            return ResponseEntity.notFound().build(); // HTTP 404 → Song existiert nicht
        }
    }

}