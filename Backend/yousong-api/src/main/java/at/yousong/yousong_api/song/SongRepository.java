package at.yousong.yousong_api.song;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    Page<SongProjection> findAllProjectedBy(Pageable pageable);

    @Query("""
            select s from Song s
            left join s.genres g
            where lower(s.title) like lower(concat('%', :q, '%'))
               or lower(s.artist.name) like lower(concat('%', :q, '%'))
               or lower(g) like lower(concat('%', :q, '%'))
            group by s
            """)
    List<SongProjection> searchProjected(@Param("q") String query);

    @Query("""
            select s from Song s
            left join s.genres g
            where (:q is null or
                   lower(s.title) like lower(concat('%', :q, '%')) or
                   lower(s.artist.name) like lower(concat('%', :q, '%')) or
                   lower(g) like lower(concat('%', :q, '%')))
            group by s
            """)
    Page<SongProjection> catalogNoGenres(@Param("q") String q, Pageable pageable);

    @Query("""
            select s from Song s
            left join s.genres g
            where (:q is null or
                   lower(s.title) like lower(concat('%', :q, '%')) or
                   lower(s.artist.name) like lower(concat('%', :q, '%')) or
                   lower(g) like lower(concat('%', :q, '%')))
            group by s
            having count(distinct case when lower(g) in :genres then lower(g) end) = :n
            """)
    Page<SongProjection> catalogWithGenres(@Param("q") String q,
                                           @Param("genres") List<String> genres,
                                           @Param("n") long n,
                                           Pageable pageable);
}
