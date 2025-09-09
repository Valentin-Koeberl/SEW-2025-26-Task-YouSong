package at.yousong.yousong_api.song;

import at.yousong.yousong_api.artist.Artist;
import at.yousong.yousong_api.artist.ArtistRepository;
import at.yousong.yousong_api.user.Benutzer;
import at.yousong.yousong_api.user.BenutzerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(20)
public class DataLoader implements CommandLineRunner {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final BenutzerRepository benutzerRepository;

    public DataLoader(SongRepository songRepository, ArtistRepository artistRepository, BenutzerRepository benutzerRepository) {
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
        this.benutzerRepository = benutzerRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (songRepository.count() > 0) {
            return;
        }

        Artist ed      = getOrCreateArtist("Ed Sheeran", "UK singer-songwriter");
        Artist queen   = getOrCreateArtist("Queen", "Legendary British rock band");
        Artist taylor  = getOrCreateArtist("Taylor Swift", "US singer-songwriter and producer");
        Artist weeknd  = getOrCreateArtist("The Weeknd", "Canadian singer, songwriter and producer");
        Artist dua     = getOrCreateArtist("Dua Lipa", "English and Albanian singer-songwriter");
        Artist imagine = getOrCreateArtist("Imagine Dragons", "American pop rock band");
        Artist billie  = getOrCreateArtist("Billie Eilish", "American singer and songwriter");
        Artist adele   = getOrCreateArtist("Adele", "English singer and songwriter");
        Artist coldplay= getOrCreateArtist("Coldplay", "British rock band");
        Artist eminem  = getOrCreateArtist("Eminem", "American rapper and producer");

        Benutzer owner = benutzerRepository.findByUsername("hugo").orElse(null);

        add("Shape of You",           List.of("Pop"),                233, ed,      "sandbreaker-379630.mp3", owner);
        add("Perfect",                List.of("Pop"),                263, ed,      "sandbreaker-379630.mp3", owner);
        add("Bohemian Rhapsody",      List.of("Rock"),               354, queen,   "sandbreaker-379630.mp3", owner);
        add("Love Story",             List.of("Country Pop","Pop"),  235, taylor,  "sandbreaker-379630.mp3", owner);

        add("Bad Habits",             List.of("Pop"),                231, ed,      "ed_sheeran_bad_habits.mp3", owner);
        add("Photograph",             List.of("Pop"),                258, ed,      "ed_sheeran_photograph.mp3", owner);

        add("Blank Space",            List.of("Pop"),                231, taylor,  "taylor_swift_blank_space.mp3", owner);
        add("Cruel Summer",           List.of("Synth-pop","Pop"),    178, taylor,  "taylor_swift_cruel_summer.mp3", owner);

        add("Blinding Lights",        List.of("Synthwave","Pop"),    200, weeknd,  "the_weeknd_blinding_lights.mp3", owner);
        add("Save Your Tears",        List.of("Pop"),                215, weeknd,  "the_weeknd_save_your_tears.mp3", owner);

        add("Levitating",             List.of("Disco","Pop"),        203, dua,     "dua_lipa_levitating.mp3", owner);
        add("Don't Start Now",        List.of("Disco","Pop"),        183, dua,     "dua_lipa_dont_start_now.mp3", owner);

        add("Believer",               List.of("Alternative Rock","Pop Rock"), 204, imagine, "imagine_dragons_believer.mp3", owner);
        add("Thunder",                List.of("Pop Rock"),           187, imagine, "imagine_dragons_thunder.mp3", owner);

        add("bad guy",                List.of("Electropop","Pop"),   194, billie,  "billie_eilish_bad_guy.mp3", owner);
        add("everything i wanted",    List.of("Pop"),                245, billie,  "billie_eilish_everything_i_wanted.mp3", owner);

        add("Hello",                  List.of("Soul","Pop"),         295, adele,   "adele_hello.mp3", owner);
        add("Rolling in the Deep",    List.of("Soul","Pop"),         228, adele,   "adele_rolling_in_the_deep.mp3", owner);

        add("Viva La Vida",           List.of("Baroque Pop","Pop"),  242, coldplay,"coldplay_viva_la_vida.mp3", owner);
        add("Fix You",                List.of("Alternative Rock"),   294, coldplay,"coldplay_fix_you.mp3", owner);

        add("Lose Yourself",          List.of("Hip Hop","Rap"),      326, eminem,  "eminem_lose_yourself.mp3", owner);
        add("The Real Slim Shady",    List.of("Hip Hop","Rap"),      284, eminem,  "eminem_the_real_slim_shady.mp3", owner);
    }

    private void add(String title, List<String> genres, int lengthSeconds, Artist artist, String fileName, Benutzer owner) {
        songRepository.save(new Song(
                null,
                title,
                genres,
                lengthSeconds,
                artist,
                fileName,
                owner
        ));
    }

    private Artist getOrCreateArtist(String name, String description) {
        return artistRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> artistRepository.save(new Artist(null, name, description)));
    }
}
