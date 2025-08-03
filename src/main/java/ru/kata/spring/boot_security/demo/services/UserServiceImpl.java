package ru.kata.spring.boot_security.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.models.Role;
import ru.kata.spring.boot_security.demo.models.User;
import ru.kata.spring.boot_security.demo.repositories.RoleRepository;
import ru.kata.spring.boot_security.demo.repositories.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RegistrationService registrationService;
    private final RoleRepository roleRepository;

    @Autowired
    public UserServiceImpl(@Lazy UserRepository userRepository, @Lazy RegistrationService registrationService, @Lazy RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.registrationService = registrationService;
        this.roleRepository = roleRepository;
    }

    @Override
    public List<User> index() {
        return userRepository.findAll();
    }

    @Override
    public User show(int id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void update(int id, User updatedUser) {
        if (updatedUser == null || id <= 0) {
            throw new IllegalArgumentException("Invalid user or ID");
        }
        updatedUser.setId(id);
        userRepository.save(updatedUser);
    }

    @Override
    @Transactional
    public void delete(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByName(String name) {
        return userRepository.findByName(name);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByName(username);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        return user.get();
    }

    @Override
    public User getCurrentUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return findByName(currentUsername).orElse(null);
    }

    @Override
    @Transactional
    public User createUser(User user) {
        Optional<User> existingUser = findByName(user.getName());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        registrationService.register(user);
        return user;
    }

    @Override
    @Transactional
    public User updateUser(int id, String name, String password, String email, int age, String role) {
        if (id <= 0 || name == null || email == null || role == null) {
            throw new IllegalArgumentException("Invalid parameters");
        }

        User existingUser = show(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("User not found");
        }

        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        user.setPassword(password != null && password.length() > 3 ? registrationService.encodePassword(password) : existingUser.getPassword());

        Role userRole = roleRepository.findByName("ROLE_" + role).orElseGet(() -> roleRepository.save(new Role("ROLE_" + role)));
        user.setRoles(Collections.singleton(userRole));

        update(id, user);
        return user;
    }

    @Override
    @Transactional
    public void deleteUser(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        User existingUser = show(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("User not found");
        }
        delete(id);
    }
}
