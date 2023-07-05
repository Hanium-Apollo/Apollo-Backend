package com.Teletubbies.Apollo.auth.service;

import com.Teletubbies.Apollo.auth.domain.User;
import com.Teletubbies.Apollo.auth.dto.MemberInfoResponse;
import com.Teletubbies.Apollo.auth.repository.UserRepository;
import com.Teletubbies.Apollo.core.exception.ApolloException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.Teletubbies.Apollo.core.exception.CustomErrorCode.INVALID_USER_ERROR;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    public void saveUser(MemberInfoResponse memberInfoResponse) {
        userRepository.save(memberInfoResponse.changeDTOtoObj(memberInfoResponse));
    }
    public User getUserById(Long id){
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser != null && optionalUser.isPresent()) return optionalUser.get();
        else throw new ApolloException(INVALID_USER_ERROR, "존재하지 않는 회원입니다");
    }
    public User getUserByLogin(String login){
        Optional<User> optionalUser = userRepository.findByLogin(login);
        if (optionalUser != null && optionalUser.isPresent()) return optionalUser.get();
        else throw new ApolloException(INVALID_USER_ERROR, "존재하지 않는 회원입니다");
    }
    public User getUserByName(String name){
        Optional<User> optionalUser = userRepository.findByName(name);
        if (optionalUser != null && optionalUser.isPresent()) return optionalUser.get();
        else throw new ApolloException(INVALID_USER_ERROR, "존재하지 않는 회원입니다");
    }

}
