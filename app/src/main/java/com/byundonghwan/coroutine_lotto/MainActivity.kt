package com.byundonghwan.coroutine_lotto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.byundonghwan.coroutine_lotto.databinding.ActivityMainBinding
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

import kotlin.collections.ArrayList
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private val job = Job() // 코루틴을 취소하기위해 job 설정.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.generateBtn.setOnClickListener {
            val lottoNumbers : ArrayList<Int> = createLottoNumbers()
            Log.d("MainActivity", lottoNumbers.toString())
            updateLottoBallImage(lottoNumbers)

            // 동행복권 json을 받아와서 코루틴 비동기 처리.-> 코루틴 취소를 위해 Dispatchers.IO + job 설정.
            CoroutineScope(Dispatchers.IO + job).launch {
                val winningNumbers = async { getLottoNumbers() } // 작업을 처리하고 deferred 객체반환
                val rank = whatIsRank(lottoNumbers, winningNumbers.await())  // 네트워크를 통해서 데이터를 다 받아올 때까지 대기.
                val text = "${winningNumbers.await()} : $rank"
                // 스레드 context를 메인ui로 잠시 전환하여 처리
                withContext(Dispatchers.Main) {
                    binding.tvWinning.text = text
                }
            }
        }
    }
    // 생명주기 종료시 코루틴 취소.
    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun createLottoNumbers() : ArrayList<Int>{
        val result = arrayListOf<Int>()

        val source = IntArray(45){it + 1} // 1 ~ 46
        val seed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.KOREA).format(Date()).hashCode().toLong()
        val random = Random(seed)
        source.shuffle(random)
        source.slice(0..5).forEach{
            result.add(it)
        }
        result.sort()

        var evenNumberCount = 0
        var oddNumberCount = 0
        for (num in result){
            if(num % 2 == 0){
                evenNumberCount += 1
            }else{
                oddNumberCount += 1
            }
        }
        result.add(result.sum())
        result.add(oddNumberCount)
        result.add(evenNumberCount)

        return result
    }

    private fun getDrawableId(number: Int) :Int {
        val number = String.format("%02d", number)
        val string = "ball_$number"
        val id = resources.getIdentifier(string, "drawable", packageName)
        return id
    }

    private fun updateLottoBallImage(result : ArrayList<Int>){
        with(binding){
            ivGame0.setImageResource(getDrawableId(result[0]))
            ivGame1.setImageResource(getDrawableId(result[1]))
            ivGame2.setImageResource(getDrawableId(result[2]))
            ivGame3.setImageResource(getDrawableId(result[3]))
            ivGame4.setImageResource(getDrawableId(result[4]))
            ivGame5.setImageResource(getDrawableId(result[5]))
            tvAnalyze.text = "번호합 : ${result[6]} 홀:짝 = ${result[7]} : ${result[8]}"
        }
    }

    private suspend fun getLottoNumbers(): ArrayList<Int> {
        val round = "1055"
        val url = "https://dhlottery.co.kr/common.do?method=getLottoNumber&drwNo=$round"
        val lottoNumbers = ArrayList<Int>()

        try {
            val response = URL(url).readText()
            val jsonObject = JsonParser.parseString(response).asJsonObject
            val returnValue = jsonObject.get("returnValue").asString

            if (returnValue == "success") {
                for (i in 1..6) {
                    val lottoNumber = jsonObject.get("drwtNo$i").asInt
                    lottoNumbers.add(lottoNumber)
                }
                val bonusNumber = jsonObject.get("bnusNo").asInt
                lottoNumbers.add(bonusNumber)
                lottoNumbers.add(round.toInt())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return lottoNumbers
    }

    private fun whatIsRank(lottoNumbers : ArrayList<Int>, winningNumber : ArrayList<Int>): String {
        var matchCount = 0
        for(i in 0..5){
            if(lottoNumbers.contains(winningNumber[i])){
                matchCount+=1
            }
        }

        return if(matchCount == 6){
            "1등"
        } else if(matchCount == 5){
            if(lottoNumbers.contains(winningNumber[6])){
                "2등"
            }else{
                "3등"
            }
        }else if(matchCount == 4){
            "4등"
        }else if(matchCount == 3){
            "5등"
        }else{
            "낙첨"
        }

    }
}