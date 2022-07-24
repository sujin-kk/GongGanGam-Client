package com.example.gonggangam.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gonggangam.util.*
import com.example.gonggangam.authService.AuthResponse
import com.example.gonggangam.authService.AuthRetrofitInterface
import com.example.gonggangam.authService.LoginBody
import com.example.gonggangam.databinding.ActivityLoginBinding
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.auth.model.Prompt
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    lateinit var binding : ActivityLoginBinding
    var kakaoAccessToken : String = ""
    var naverAccessToken: String = ""
    var jwt: String = ""
    var userIdx: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListener()
        loadDeviceToken()
    }

    fun initListener() {
        binding.loginKakaoBtn.setOnClickListener {
            onKakao()
        }

        binding.loginNaverBtn.setOnClickListener {
            onNaver()
        }
    }

    fun onNaver() {
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                // 네이버 로그인 인증이 성공했을 때 수행할 코드 추가
                naverAccessToken = NaverIdLoginSDK.getAccessToken().toString()
                Log.d("TAG-API-NAVER", naverAccessToken)
                naverLogin() // API link start
            }
            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                Toast.makeText(this@LoginActivity,"errorCode:$errorCode, errorDesc:$errorDescription",Toast.LENGTH_SHORT).show()
            }
            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        }
        NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
    }

    fun naverLogin() {
        val body = LoginBody(naverAccessToken, PrefManager.deviceToken)
        val authService = getRetrofit().create(AuthRetrofitInterface::class.java)
        Log.d("NAVER-API", body.toString())

        authService.naverLogin(body).enqueue(object: Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                Log.d("NAVER/API-RESPONSE", response.toString())

                if(response.isSuccessful && response.code() == 200) {
                    val resp = response.body()!!
                    Log.d("NAVER/API-RESPONSE-RESP", resp.toString())

                    userIdx = resp.result!!.userIdx
                    jwt = resp.result.jwt
                    Log.d("test", jwt)

                    when(resp.code) {
                        1000 -> {
                            Toast.makeText(this@LoginActivity,"로그인 성공",Toast.LENGTH_SHORT).show()
                            goToMainActivity()
                        }
                        5028 -> goToAdditionalInfo("naver") // sign in
                        else -> Toast.makeText(this@LoginActivity,"네이버 로그인 실패",Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.d("LOGIN/API-ERROR", t.message.toString())
            }

        })
    }

    fun onKakao(){
        // 카카오 서버 응답에 대한 callback
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.d("TAG_KAKAO", "카카오계정으로 로그인 실패", error)
            } else if (token != null) {
                Log.d("TAG_KAKAO", "카카오계정으로 로그인 성공 ${token.accessToken}")
                kakaoAccessToken = token.accessToken
                kakaoLogin()
            }
        }

        // 카카오톡 앱이 존재하면 열기
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    Log.d("TAG_KAKAO", "카카오톡으로 로그인 실패", error)

                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(this, prompts = listOf(Prompt.LOGIN), callback = callback)
                } else if (token != null) {
                    Log.d("TAG_KAKAO", "카카오톡으로 로그인 성공 ${token.accessToken}")
                    kakaoAccessToken = token.accessToken
                    kakaoLogin()
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    fun kakaoLogin() {
        val body = LoginBody(token = kakaoAccessToken, deviceToken = PrefManager.deviceToken)
        val authService = getRetrofit().create(AuthRetrofitInterface::class.java)
        Log.d("KAKAO-API", body.toString())

        authService.kakaoLogin(body).enqueue(object: Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                Log.d("KAKAO/API-RESPONSE", response.toString())

                if(response.isSuccessful && response.code() == 200) {
                    val resp = response.body()!!
                    Log.d("KAKAO/API-RESPONSE-RESP", resp.toString())

                    when(resp.code) {
                        1000 -> {
                            // save userIdx & jwt
                            userIdx = resp.result!!.userIdx
                            jwt = resp.result.jwt

                            Toast.makeText(this@LoginActivity,"로그인 성공",Toast.LENGTH_SHORT).show()
                            goToMainActivity()
                        }
                        5028 -> goToAdditionalInfo("kakao") // sign in
                        else -> Toast.makeText(this@LoginActivity,"카카오 로그인 실패",Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.d("KAKAO/API-ERROR", t.message.toString())
            }

        })
    }

    fun goToAdditionalInfo(type: String) {
        val intent = Intent(this, AdditionalInformationActivity::class.java)
        intent.putExtra("type", type) // kakao or naver
        intent.putExtra("jwt", jwt) // jwt
        intent.putExtra("userIdx", userIdx) // userIdx
        startActivity(intent)
        finish()
    }

    fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)

        // user 정보 저장 후 main 진입
        PrefManager.setAuth(jwt, userIdx)
        startActivity(intent)
        finish()
    }

    private fun loadDeviceToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (!it.isSuccessful) {
                Log.w("TAG-MAIN", "FCM token failed", it.exception)
            } else {
                PrefManager.setDeviceToken(it.result)
                Log.d("TAG-MAIN", "FCM token : ${it.result}")
            }
        }
    }

}
