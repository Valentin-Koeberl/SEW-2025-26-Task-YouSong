package at.yousong.yousong_api.song;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SongController {

    private final SongRepository repository;

    public SongController(SongRepository repository) {
        this.repository = repository;
    }

    @CrossOrigin // für Frontend-Zugriff notwendig, z.B. Vue später
    @GetMapping("/songs")
    public List<Song> getAllSongs() {
        return repository.findAll();
    }
}
