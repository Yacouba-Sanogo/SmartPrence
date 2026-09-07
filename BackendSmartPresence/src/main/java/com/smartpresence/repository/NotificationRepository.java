package com.smartpresence.repository; import com.smartpresence.entity.Notification; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification,UUID>{List<Notification> findByDestinataireIdOrderByCreatedAtDesc(UUID destinataireId); long countByDestinataireIdAndLueFalse(UUID destinataireId);}
