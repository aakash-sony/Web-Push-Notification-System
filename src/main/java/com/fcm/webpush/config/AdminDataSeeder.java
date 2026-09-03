package com.fcm.webpush.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fcm.webpush.entity.NotificationMaster;
import com.fcm.webpush.entity.User;
import com.fcm.webpush.enums.NotificationType;
import com.fcm.webpush.repository.NotificationMasterRepository;
import com.fcm.webpush.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminDataSeeder implements ApplicationRunner {

	private final UserRepository 					userRepository;
	private final NotificationMasterRepository 		notificationMasterRepository;
	private final PasswordEncoder passwordEncoder 	= new BCryptPasswordEncoder();

	@Value("${admin.username}")
	private String adminUsername;

	@Value("${admin.password}")
	private String adminPassword;

	@Override
	@Transactional
	public void run(final ApplicationArguments args) {
		seedAdminUser();
		seedDefaultTemplates();
	}

	private void seedAdminUser() {
		if (userRepository.existsByUsername(adminUsername))
			return;

		final var encodedPassword 		= passwordEncoder.encode(adminPassword);
		final var adminUser 			= User.builder().username(adminUsername).password(encodedPassword).build();

		userRepository.save(adminUser);
	}

	private void seedDefaultTemplates() {
		if (notificationMasterRepository.count() > 0)
			return;

		final var welcomeTemplate =	NotificationMaster.builder()
				.code(NotificationType.WELCOME)
				.title("Welcome to our platform")
				.bodyTemplate("Welcome! We are happy to have you.")
				.isActive(true)
				.build();

		final var promotionalTemplate = NotificationMaster.builder()
				.code(NotificationType.PROMOTIONAL)
				.title("Special Offer")
				.bodyTemplate("Check out our latest offers and deals!")
				.isActive(true)
				.build();

		final var alertTemplate = NotificationMaster.builder()
				.code(NotificationType.SYSTEM_ALERT)
				.title("System Update")
				.bodyTemplate("Important update regarding system services.")
				.isActive(true)
				.build();

		notificationMasterRepository.save(welcomeTemplate);
		notificationMasterRepository.save(promotionalTemplate);
		notificationMasterRepository.save(alertTemplate);

	}
}
