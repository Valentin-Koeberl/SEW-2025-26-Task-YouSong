package at.yousong.yousong_api.song;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final SongRepository repository;

    public DataLoader(SongRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                    new Song(null, "Blinding Lights", "The Weeknd", "Pop", 200),
                    new Song(null, "Numb", "Linkin Park", "Rock", 185),
                    new Song(null, "Levitating", "Dua Lipa", "Pop", 203)
            ));
        }
    }
}
