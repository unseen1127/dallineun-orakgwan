package kr.unseen.dallineun.game

/** 게임 콘텐츠. 시제품용 내장 데이터이며, 서비스 단계에서 서버 콘텐츠로 교체한다. */
object Content {

    /** 끝말잇기 시작 단어 */
    val startWords = listOf("기차", "바다", "사과", "나무", "자동차", "고구마", "도로", "휴게소", "여행", "구름", "하늘", "라디오")

    data class Chosung(val answer: String, val category: String, val hint: String)

    val chosungQuiz = listOf(
        Chosung("김치찌개", "음식", "김치가 들어간 얼큰한 찌개"),
        Chosung("떡볶이", "음식", "빨간 양념에 떡을 볶은 분식"),
        Chosung("붕어빵", "간식", "겨울 길거리 생선 모양 빵"),
        Chosung("호두과자", "간식", "휴게소 단골 간식"),
        Chosung("자동차", "탈것", "지금 타고 있는 것"),
        Chosung("고속도로", "장소", "지금 달리는 길"),
        Chosung("휴게소", "장소", "잠깐 쉬어 가는 곳"),
        Chosung("신호등", "도로", "빨강 노랑 초록"),
        Chosung("무지개", "자연", "비 온 뒤 일곱 빛깔"),
        Chosung("눈사람", "겨울", "눈으로 만든 사람"),
        Chosung("크리스마스", "기념일", "12월 25일"),
        Chosung("아이스크림", "간식", "차갑고 달콤한 디저트"),
        Chosung("해바라기", "꽃", "해를 따라 도는 꽃"),
        Chosung("텔레비전", "물건", "거실의 화면"),
        Chosung("손흥민", "인물", "대한민국 축구 대표팀 주장"),
        Chosung("세종대왕", "인물", "한글을 만든 임금"),
    )

    data class RegionQ(val q: String, val answers: List<String>, val explain: String)
    data class Region(val name: String, val lat: Double, val lng: Double, val keys: List<String>, val qs: List<RegionQ>)

    val regions = listOf(
        Region("천안", 36.815, 127.114, listOf("천안"), listOf(
            RegionQ("천안의 대표 간식, 호두를 넣어 구운 과자는 무엇일까요?", listOf("호두과자", "호두"), "정답은 호두과자! 천안 호두과자는 휴게소 단골 간식이죠."),
            RegionQ("천안에 있는, 독립운동의 역사를 기록한 국립 기념관은?", listOf("독립기념관", "독립"), "정답은 독립기념관입니다."),
        )),
        Region("대전", 36.350, 127.385, listOf("대전"), listOf(
            RegionQ("1993년 대전에서 열린 국제 박람회의 이름은?", listOf("엑스포", "대전엑스포", "꿈돌이"), "정답은 대전 엑스포! 마스코트는 꿈돌이였어요."),
        )),
        Region("전주", 35.824, 127.148, listOf("전주"), listOf(
            RegionQ("전주를 대표하는 음식, 밥 위에 나물을 얹어 비벼 먹는 이 음식은?", listOf("비빔밥", "전주비빔밥"), "정답은 전주비빔밥!"),
            RegionQ("전주의 전통 한옥이 모여 있는 관광 명소는?", listOf("한옥마을", "한옥"), "정답은 전주 한옥마을입니다."),
        )),
        Region("강릉", 37.752, 128.876, listOf("강릉"), listOf(
            RegionQ("강릉 초당동에서 바닷물로 간을 해 만든다는 두부 요리는?", listOf("순두부", "초당순두부", "초당두부"), "정답은 초당순두부!"),
            RegionQ("율곡 이이가 태어난 강릉의 집, 검은 대나무로 유명한 이곳은?", listOf("오죽헌"), "정답은 오죽헌입니다."),
        )),
        Region("평창", 37.370, 128.390, listOf("평창"), listOf(
            RegionQ("2018년 평창에서 열린 국제 스포츠 대회는?", listOf("동계올림픽", "올림픽"), "정답은 평창 동계올림픽!"),
        )),
        Region("경주", 35.856, 129.224, listOf("경주"), listOf(
            RegionQ("경주는 어느 나라의 천 년 수도였을까요?", listOf("신라"), "정답은 신라!"),
            RegionQ("경주에 있는 신라 시대의 천문 관측대 이름은?", listOf("첨성대"), "정답은 첨성대입니다."),
        )),
        Region("부산", 35.180, 129.076, listOf("부산"), listOf(
            RegionQ("부산의 대표 음식, 뽀얀 국물에 밥을 말아 먹는 국밥은?", listOf("돼지국밥", "돼지"), "정답은 돼지국밥!"),
        )),
        Region("여수", 34.760, 127.662, listOf("여수"), listOf(
            RegionQ("버스커버스커의 노래 제목, 여수 ○○○. 빈칸은?", listOf("밤바다"), "정답은 여수 밤바다!"),
        )),
        Region("춘천", 37.881, 127.730, listOf("춘천"), listOf(
            RegionQ("춘천의 대표 음식, 닭고기를 양념해 철판에 볶는 요리는?", listOf("닭갈비"), "정답은 춘천 닭갈비!"),
        )),
        Region("수원", 37.263, 127.029, listOf("수원"), listOf(
            RegionQ("조선 정조 때 쌓은 수원의 성곽 이름은?", listOf("화성", "수원화성"), "정답은 수원 화성입니다."),
        )),
        Region("안동", 36.568, 128.729, listOf("안동"), listOf(
            RegionQ("유네스코 세계유산인 안동의 전통 마을 이름은?", listOf("하회마을", "하회"), "정답은 하회마을!"),
        )),
        Region("서울", 37.566, 126.978, listOf("서울"), listOf(
            RegionQ("광화문 뒤에 있는 조선의 으뜸 궁궐은?", listOf("경복궁"), "정답은 경복궁입니다."),
        )),
        Region("광주", 35.160, 126.851, listOf("광주광역시", "광주 "), listOf(
            RegionQ("광주를 대표하는 산이자 국립공원은?", listOf("무등산"), "정답은 무등산!"),
        )),
        Region("통영", 34.854, 128.433, listOf("통영"), listOf(
            RegionQ("통영에서 시작된, 맨밥 김밥과 반찬을 따로 먹는 김밥은?", listOf("충무김밥", "충무"), "정답은 충무김밥!"),
        )),
        Region("담양", 35.321, 126.988, listOf("담양"), listOf(
            RegionQ("담양의 유명한 대나무 숲 정원 이름은?", listOf("죽녹원"), "정답은 죽녹원입니다."),
        )),
    )

    /** 위치를 모를 때 쓰는 전국 퀴즈 */
    val nationwide = listOf(
        RegionQ("우리나라에서 가장 높은 산은?", listOf("한라산"), "정답은 한라산!"),
        RegionQ("우리나라에서 가장 긴 강은?", listOf("낙동강"), "정답은 낙동강입니다."),
        RegionQ("경부고속도로는 서울과 어느 도시를 잇는 길일까요?", listOf("부산"), "정답은 부산!"),
        RegionQ("독도가 속한 군은?", listOf("울릉", "울릉군"), "정답은 경상북도 울릉군입니다."),
    )

    /** 주소 문자열 또는 좌표로 현재 지역을 찾는다 */
    fun findRegion(address: String?, lat: Double?, lng: Double?): Region? {
        if (!address.isNullOrBlank()) {
            regions.firstOrNull { r -> r.keys.any { address.contains(it.trim()) } }?.let { return it }
        }
        if (lat != null && lng != null) {
            val nearest = regions.minByOrNull { dist(lat, lng, it.lat, it.lng) }
            if (nearest != null && dist(lat, lng, nearest.lat, nearest.lng) < 35.0) return nearest
        }
        return null
    }

    private fun dist(a: Double, b: Double, c: Double, d: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(c - a); val dLng = Math.toRadians(d - b)
        val h = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(a)) * Math.cos(Math.toRadians(c)) * Math.sin(dLng / 2).let { it * it }
        return 2 * r * Math.asin(Math.sqrt(h))
    }
}
