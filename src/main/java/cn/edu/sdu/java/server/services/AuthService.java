package cn.edu.sdu.java.server.services;

import cn.edu.sdu.java.server.models.*;
import cn.edu.sdu.java.server.payload.request.DataRequest;
import cn.edu.sdu.java.server.payload.request.LoginRequest;
import cn.edu.sdu.java.server.payload.response.DataResponse;
import cn.edu.sdu.java.server.payload.response.JwtResponse;
import cn.edu.sdu.java.server.repositorys.*;
import cn.edu.sdu.java.server.util.CommonMethod;
import cn.edu.sdu.java.server.util.DateTimeTool;
import cn.edu.sdu.java.server.util.LoginControlUtil;
import jakarta.validation.Valid;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
// 其他 import 保持不变

@Service
public class AuthService {
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final UserTypeRepository userTypeRepository;
    private final StudentRepository studentRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;

    public AuthService(PersonRepository personRepository, UserRepository userRepository, UserTypeRepository userTypeRepository, StudentRepository studentRepository,AuthenticationManager authenticationManager, JwtService jwtService, PasswordEncoder encoder, ResourceLoader resourceLoader) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
        this.userTypeRepository = userTypeRepository;
        this.studentRepository = studentRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.encoder = encoder;
    }
    /*public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        System.out.println("0");
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        System.out.println("1");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("2");
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        System.out.println("3");
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        System.out.println("4");
        Optional<User> op= userRepository.findByUserName(loginRequest.getUsername());
        System.out.println("5");
        if(op.isPresent()) {
            User user= op.get();
            System.out.println("6");
            user.setLastLoginTime(DateTimeTool.parseDateTime(new Date()));
            System.out.println("7");
            Integer count = user.getLoginCount();
            if (count == null)
                count = 1;
            else count += 1;
            user.setLoginCount(count);
            userRepository.save(user);
        }
        String jwt = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getPerName(),
                roles.getFirst()));
    }*/


    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        System.out.println("0: before authenticate, username=" + loginRequest.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            System.out.println("1: after authenticate");

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            Optional<User> op = userRepository.findByUserName(loginRequest.getUsername());
            if (op.isPresent()) {
                User user = op.get();
                user.setLastLoginTime(DateTimeTool.parseDateTime(new Date()));
                Integer count = user.getLoginCount();
                user.setLoginCount(count == null ? 1 : (count + 1));
                userRepository.save(user);
            }

            String jwt = jwtService.generateToken(userDetails);
            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getPerName(),
                    roles.isEmpty() ? "" : roles.getFirst()));

        } catch (AuthenticationException e) {
            // 关键：把真实原因打印出来
            System.out.println("!!! AUTH FAILED: " + e.getClass().getSimpleName() + " msg=" + e.getMessage());
            e.printStackTrace();

            // 登录失败更合理是 401（未认证），而不是 403（已认证但没权限）
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login failed: " + e.getClass().getSimpleName());
        } catch (Exception e) {
            // 万一你自定义 provider / 取用户信息等地方抛了别的异常
            System.out.println("!!! LOGIN ERROR: " + e.getClass().getName() + " msg=" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Login error: " + e.getClass().getSimpleName());
        }
    }

    public DataResponse getValidateCode(DataRequest dataRequest) {
        return CommonMethod.getReturnData(LoginControlUtil.getInstance().getValidateCodeDataMap());
    }

    public DataResponse testValidateInfo( DataRequest dataRequest) {
        Integer validateCodeId = dataRequest.getInteger("validateCodeId");
        String validateCode = dataRequest.getString("validateCode");
        LoginControlUtil li =  LoginControlUtil.getInstance();
        if(validateCodeId == null || validateCode== null || validateCode.isEmpty()) {
            return CommonMethod.getReturnMessageError("验证码为空！");
        }
        String value = li.getValidateCode(validateCodeId);
        if(!validateCode.equals(value))
            return CommonMethod.getReturnMessageError("验证码错位！");
        return CommonMethod.getReturnMessageOK();
    }
    /*
     *  注册用户示例，我们项目暂时不用， 所有用户通过管理员添加，这里注册，没有考虑关联人员信息的创建，使用时参加学生添加功能的实现
     */
    @PostMapping("/registerUser")
    public DataResponse registerUser(@Valid @RequestBody DataRequest dataRequest) {
        String username = dataRequest.getString("username");
        String password = dataRequest.getString("password");
        String perName = dataRequest.getString("perName");
        String email = dataRequest.getString("email");
        String role = dataRequest.getString("role");
        UserType ut = null;
        Optional<User> uOp = userRepository.findByUserName(username);
        if(uOp.isPresent()) {
            return CommonMethod.getReturnMessageError("用户已经存在，不能注册！");
        }
        Person p = new Person();
        p.setNum(username);
        p.setName(perName);
        p.setEmail(email);
        if("ADMIN".equals(role)) {
            p.setType("0");
            ut = userTypeRepository.findByName(EUserType.ROLE_ADMIN.name());
        }else if("STUDENT".equals(role)) {
            p.setType("1");
            ut = userTypeRepository.findByName(EUserType.ROLE_STUDENT.name());
        }else if("TEACHER".equals(role)) {
            p.setType("2");
            ut = userTypeRepository.findByName(EUserType.ROLE_TEACHER.name());
        }
        personRepository.saveAndFlush(p);
        User u = new User();
        u.setPerson(p);
        u.setUserType(ut);
        u.setUserName(username);
        u.setPassword(encoder.encode(password));
        u.setCreateTime(DateTimeTool.parseDateTime(new Date()));
        u.setCreatorId(p.getPersonId());
        u.setLoginCount(0);
        userRepository.saveAndFlush(u);
        if("STUDENT".equals(role)) {
            Student s = new Student();   // 创建实体对象
            s.setPerson(p);
            studentRepository.saveAndFlush(s);  //插入新的Student记录
        }
        return CommonMethod.getReturnData(LoginControlUtil.getInstance().getValidateCodeDataMap());
    }

}
