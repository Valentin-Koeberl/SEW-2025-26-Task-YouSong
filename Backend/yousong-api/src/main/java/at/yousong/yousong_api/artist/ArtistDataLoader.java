package at.yousong.yousong_api.artist;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"default"})
public class ArtistDataLoader implements CommandLineRunner {

    private final ArtistRepository artistRepository;

    public ArtistDataLoader(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @Override
    public void run(String... args) {
        if (artistRepository.count() == 0) {
            artistRepository.save(new Artist(null, "Ed Sheeran", "UK singer-songwriter"));
            artistRepository.save(new Artist(null, "Queen", "Legendary British rock band"));
            artistRepository.save(new Artist(null, "Taylor Swift", "US singer-songwriter and producer"));
        }
    }
}
