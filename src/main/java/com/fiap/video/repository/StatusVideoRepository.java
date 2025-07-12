package com.fiap.video.repository;

import com.fiap.video.entity.StatusVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusVideoRepository extends JpaRepository<StatusVideo, Long> {

}
