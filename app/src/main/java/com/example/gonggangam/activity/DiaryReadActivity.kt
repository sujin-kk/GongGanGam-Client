package com.example.gonggangam.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.gonggangam.diaryService.DiaryRetrofitInterface
import com.example.gonggangam.diaryService.ReadDiaryResponse
import com.example.gonggangam.util.BindingAdapter
import com.example.gonggangam.util.getRetrofit
import com.example.gonggangam.R
import com.example.gonggangam.databinding.ActivityDiaryReadBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DiaryReadActivity : AppCompatActivity() {
    lateinit var binding: ActivityDiaryReadBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryReadBinding.inflate(layoutInflater)
        getData()
        initListener()
        setContentView(binding.root)
    }

    private fun getData() {
        val year = intent.getIntExtra("year", -1)
        val month = intent.getIntExtra("month", -1)
        val day = intent.getIntExtra("day", -1)

        val diaryService = getRetrofit().create(DiaryRetrofitInterface::class.java)
        diaryService.getDiary(year, month, day).enqueue(object: Callback<ReadDiaryResponse> {
            override fun onResponse(
                call: Call<ReadDiaryResponse>,
                response: Response<ReadDiaryResponse>
            ) {
                if(response.isSuccessful && response.code() == 200) {
                    val resp = response.body()!!
                    Log.d("TAG/API-RESPONSE", resp.toString())

                    when(resp.code) {
                        1000 -> {
                            val emoji: String  = resp.result.emoji
                            val date: String  = resp.result.diaryDate
                            val contents: String  = resp.result.contents
                            val img: String?  = resp.result.image
                            val answer = resp.result.answer

                            // Diary Binding
                            BindingAdapter.loadEmoji(emoji, binding.diaryReadMoodIv)
                            binding.diaryReadDateTv.text = date
                            binding.diaryReadContentTv.text = contents
                            if(img != null) {
                                BindingAdapter.loadDiaryImage(img, binding.diaryReadContentIv)
                                binding.diaryReadContentIv.visibility = View.VISIBLE
                            }

                            // Answer Binding
                            if(answer != null) {
                                binding.diaryReadMyCl.visibility = View.VISIBLE
                                BindingAdapter.loadProfileImage(answer.userProfImg,
                                    binding.diaryReadSenderProfileIv,
                                    ContextCompat.getDrawable(this@DiaryReadActivity, R.drawable.default_profile_img)!!
                                )
                                binding.diaryReadSendDate.text = answer.answerTime
                                binding.diaryReadSender.text = answer.nickname
                                binding.diaryReadMyContent.text = answer.answerContent
                            }
                        }
                        else -> Log.d("TAG/API-CODE", "다이어리 로드 실패" )
                    }
                }
            }

            override fun onFailure(call: Call<ReadDiaryResponse>, t: Throwable) {
                Log.d("TAG-API/ERROR", t.message.toString())
            }
        })
    }

    private fun initListener() {
        binding.diaryReadHeader.layoutHeaderBackIv.setOnClickListener {
            finish()
        }

        //헤더 내의 내용 수정
        binding.diaryReadHeader.layoutHeaderBtnTv.text= "수정"
        binding.diaryReadHeader.layoutHeaderMenuIv.visibility= View.INVISIBLE
        binding.diaryReadHeader.layoutHeaderTitleTv.visibility= View.INVISIBLE

        binding.diaryReadHeader.layoutHeaderBackIv.setOnClickListener {
            finish()
        }

        binding.diaryReadMyCl.setOnClickListener {
            val intent = Intent(this, AcceptDiaryActivity::class.java)
            startActivity(intent)
        }
    }
}