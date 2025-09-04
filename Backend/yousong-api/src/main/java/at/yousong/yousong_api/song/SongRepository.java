package at.yousong.yousong_api.song;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByTitleContainingIgnoreCaseOrArtist_NameContainingIgnoreCase(String title, String artistName);
}
