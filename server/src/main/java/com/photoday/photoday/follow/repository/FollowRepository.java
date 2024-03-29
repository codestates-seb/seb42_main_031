package com.photoday.photoday.follow.repository;


import com.photoday.photoday.follow.entity.Follow;
import com.photoday.photoday.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);
}
