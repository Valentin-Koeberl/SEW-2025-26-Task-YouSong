package at.yousong.yousong_api.song;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@CrossOrigin(origins = "http://localhost:5173") // Vue-Dev-Server erlauben
public class SongController {


    private final SongRepository songRepository;

    public SongController(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    // ✅ GET - Alle Songs mit Paging
    @GetMapping
    public ResponseEntity<Page<Song>> getAllSongs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Song> songs = songRepository.findAll(pageable);
        return ResponseEntity.ok(songs);
    }

    @PostMapping
    public ResponseEntity<Song> createSong(@RequestBody Song newSong) {
        Song savedSong = songRepository.save(newSong);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSong);
    }


    // ✅ PUT - Song aktualisieren
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

    // ✅ DELETE - Song löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
        if (songRepository.existsById(id)) {
            songRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Suche bleibt bestehen (ohne Paging)
    @GetMapping("/search")
    public ResponseEntity<List<Song>> searchSongs(@RequestParam String query) {
        List<Song> songs = songRepository.findByTitleContainingIgnoreCaseOrArtist_NameContainingIgnoreCase(query, query);

        if (songs.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Song> getSongById(@PathVariable Long id) {
        return songRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
