package spring.apitest.presentation.v1;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import spring.apitest.application.KakaoService;
import spring.apitest.application.ResponseService;
import spring.apitest.common.config.security.JwtTokenProvider;
import spring.apitest.domain.User;
import spring.apitest.domain.UserJpaRepo;
import spring.apitest.domain.response.CommonResult;
import spring.apitest.domain.response.SingleResult;
import spring.apitest.domain.social.KakaoProfile;
import spring.apitest.domain.social.RetKakaoAuth;
import spring.apitest.exception.CEmailSigninFailedException;
import spring.apitest.exception.CUserNotFoundException;

import java.util.Collections;
import java.util.Optional;

@Api(tags = {"1. Sign"})
@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/v1")
public class SignController {

    private final UserJpaRepo userJpaRepo;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResponseService responseService;
    private final PasswordEncoder passwordEncoder;
    private final KakaoService kakaoService;

    @ApiOperation(value = "로그인", notes = "이메일 회원 로그인을 한다.")
    @PostMapping(value = "/signin")
    public SingleResult<String> signin(@ApiParam(value = "회원ID : 이메일", required = true) @RequestParam String id,
                                       @ApiParam(value = "비밀번호", required = true) @RequestParam String password) {
        User user = userJpaRepo.findByUid(id).orElseThrow(CEmailSigninFailedException::new);
        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new CEmailSigninFailedException();

        return responseService.getSingleResult(jwtTokenProvider.createToken(String.valueOf(user.getMsrl()), user.getRoles()));

    }

    @ApiOperation(value = "가입", notes = "회원가입을 한다.")
    @PostMapping(value = "/signup")
    public CommonResult signin(@ApiParam(value = "회원ID : 이메일", required = true) @RequestParam String id,
                               @ApiParam(value = "비밀번호", required = true) @RequestParam String password,
                               @ApiParam(value = "이름", required = true) @RequestParam String name) {

        userJpaRepo.save(User.builder()
                .uid(id)
                .password(passwordEncoder.encode(password))
                .name(name)
                .roles(Collections.singletonList("ROLE_USER"))
                .build());
        return responseService.getSuccessResult();
    }

    @ApiOperation(value = "소셜 로그인", notes = "소셜 회원 로그인")
    @PostMapping(value = "/signin/{provider}")
    public SingleResult<String> signinByProvider(
            @ApiParam(value = "Provider", required = true, defaultValue = "Kakao") @PathVariable String provider,
            @ApiParam(value = "access_token", required = true) @RequestParam String access_token
    ) throws Exception {
        KakaoProfile profile = kakaoService.getKakaoProfile(access_token);
        User user = userJpaRepo.findByUidAndProvider(String.valueOf(profile.getId()), provider).orElseThrow(CUserNotFoundException::new);

        return responseService.getSingleResult(jwtTokenProvider.createToken(String.valueOf(user.getMsrl()), user.getRoles()));

    }

    @ApiOperation(value = "소셜 계정 가입", notes = "소셜 계정 회원 가입")
    @PostMapping(value = "/signup/{provider}")
    public CommonResult signupProvider(@ApiParam(value = "Provider", required = true, defaultValue = "Kakao") @PathVariable String provider,
                                       @ApiParam(value = "access_token", required = true) @RequestParam String accessToken,
                                       @ApiParam(value = "name", required = true) @RequestParam String name) throws Exception{
        KakaoProfile profile = kakaoService.getKakaoProfile(accessToken);
        Optional<User> user = userJpaRepo.findByUidAndProvider(String.valueOf(profile.getId()), provider);

        if (user.isPresent()) {
            throw new CUserNotFoundException();
        }

        userJpaRepo.save(User.builder()
                .uid(String.valueOf(profile.getId()))
                .name(name)
                .provider(provider)
                .roles(Collections.singletonList("ROLE_USER"))
                .build());
        return responseService.getSuccessResult();
    }
}
