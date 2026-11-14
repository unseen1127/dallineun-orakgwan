package kr.unseen.dallineun.game

/** 게임 콘텐츠. 시제품용 내장 데이터이며, 서비스 단계에서 서버 콘텐츠로 교체한다. */
object Content {

    /** 끝말잇기 시작 단어 */
    val startWords = listOf("기차", "바다", "사과", "나무", "자동차", "고구마", "도로", "휴게소", "여행", "구름", "하늘", "라디오")

    /** 혼자 드라이브: MC가 상대로 낼 단어. 서비스 단계에서 국어사전 데이터로 교체 */
    val mcWords: List<String> = ("가방 가위 가족 가게 각도 간식 감자 강물 거울 거미 거리 건물 게임 겨울 계단 고구마 고래 고양이 공원 공책 과자 " +
        "구름 국수 군밤 그네 그림 극장 기린 기차 김밥 꽃병 나무 나비 낙타 날개 남산 냉면 노래 노을 놀이터 누나 눈사람 다리 다람쥐 " +
        "달력 당근 대문 도로 도시락 도토리 돌고래 동물 두부 딸기 라면 레몬 로봇 마당 마을 만두 망원경 머리 모자 목도리 무지개 문어 " +
        "물감 바다 바람 바지 박수 반지 밤하늘 배추 버스 벌레 별자리 병원 보물 보리 볼펜 부채 비행기 빵집 사과 사자 사진 산책 상자 " +
        "새우 생선 서랍 선물 설탕 소금 소나무 손수건 수박 숟가락 시계 시장 신발 아침 악어 안경 야구 양말 어항 얼음 여름 여우 연필 " +
        "엽서 오리 오이 옥수수 우산 우유 운동 원숭이 유리 은행 의자 이불 인형 일기 자동차 자전거 잠자리 장갑 장난감 저녁 전화 정원 " +
        "제비 조개 종이 주스 지도 지우개 집게 짜장면 참외 창문 책상 천둥 초콜릿 축구 치약 카메라 커튼 케이크 코끼리 콩나물 크레파스 " +
        "타조 탁자 태극기 토끼 통조림 튤립 파도 파랑새 팽이 편지 포도 표정 풍선 피아노 하늘 학교 한강 해바라기 햄버거 호랑이 호박 화분 휴게소")
        .split(" ")

    /* ---------- 아재개그 퀴즈 ---------- */
    data class Ajae(val q: String, val answers: List<String>, val reveal: String)

    val ajae = listOf(
        Ajae("자동차를 톡 치면?", listOf("카톡"), "정답은 카톡! 카를 톡!"),
        Ajae("소가 웃으면?", listOf("우하하"), "정답은 우하하!"),
        Ajae("왕이 넘어지면?", listOf("킹콩"), "정답은 킹콩!"),
        Ajae("오리가 얼면?", listOf("언덕"), "정답은 언덕! 언 덕(duck)!"),
        Ajae("깨가 죽으면?", listOf("주근깨"), "정답은 주근깨!"),
        Ajae("세상에서 가장 뜨거운 과일은?", listOf("천도복숭아", "천도"), "정답은 천도복숭아!"),
        Ajae("바나나가 웃으면?", listOf("바나나킥", "킥"), "정답은 바나나킥!"),
    )

    /* ---------- 스무고개 (혼자 드라이브) ---------- */

    /** facts: 예/아니요로 답할 속성. 서비스 단계에서 LLM 응답으로 확장 */
    data class Twenty(val name: String, val aliases: List<String>, val facts: Set<String>, val hints: List<String>)

    private fun tw(name: String, facts: String, vararg hints: String, aliases: List<String> = emptyList()) =
        Twenty(name, aliases + name, facts.split(" ").filter { it.isNotBlank() }.toSet(), hints.toList())

    val twenty = listOf(
        tw("기린", "동물 살아 다리 다리4 크다 목길다 털 노랑 아프리카", "목이 아주 길어요", "아프리카 초원에 살아요"),
        tw("고래", "동물 살아 크다 물 바다 헤엄", "바다에서 가장 큰 동물 중 하나예요", "물을 뿜어요"),
        tw("수박", "식물 음식 먹 과일 초록 빨강 여름 크다", "여름 과일이에요", "겉은 초록, 속은 빨강"),
        tw("자전거", "물건 탈것 바퀴 다리힘", "두 바퀴로 달려요", "페달을 밟아요"),
        tw("냉장고", "물건 집 전기 크다 차갑다", "부엌에 있어요", "안이 차가워요"),
        tw("펭귄", "동물 살아 다리 날개 물 바다 헤엄 흑백 남극", "날개가 있지만 날지 못해요", "남극에 살아요"),
        tw("우산", "물건 손 비", "비 오는 날 필요해요", "펼쳐서 써요"),
        tw("버스", "물건 탈것 바퀴 크다 전기", "여러 사람이 함께 타요", "정류장에 서요"),
        tw("호랑이", "동물 살아 다리 다리4 털 크다 노랑", "줄무늬가 있어요", "산의 왕이라고 불려요"),
        tw("딸기", "식물 음식 먹 과일 빨강 작다", "빨갛고 씨가 겉에 있어요", "케이크 위에 자주 올라가요"),
    )

    /** 질문 키워드 -> 속성. 앞쪽이 우선 */
    val twentyKeys: List<Pair<Regex, String>> = listOf(
        Regex("바다|물에|물속|헤엄") to "물",
        Regex("동물") to "동물",
        Regex("식물|나무|풀") to "식물",
        Regex("과일") to "과일",
        Regex("음식|먹") to "먹",
        Regex("탈것|타는|탈 수|탈수") to "탈것",
        Regex("살아|생물|숨") to "살아",
        Regex("날개|날아|날 수") to "날개",
        Regex("다리.*(네|4)") to "다리4",
        Regex("다리") to "다리",
        Regex("목") to "목길다",
        Regex("털") to "털",
        Regex("바퀴") to "바퀴",
        Regex("전기|배터리|충전") to "전기",
        Regex("집|부엌|방에") to "집",
        Regex("작아|작은|작니|작나") to "작다",
        Regex("커|크|큰") to "크다",
        Regex("빨간|빨강") to "빨강",
        Regex("노란|노랑") to "노랑",
        Regex("초록|녹색") to "초록",
        Regex("물건|사물") to "물건",
    )

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
        Chosung("세종대왕", "인물", "한글을 만든 임금"),
    )

    /** 지역 퀴즈: 객관식(3지선다) 또는 OX. 운전 중에도 부담 없이 답할 수 있는 형식 */
    sealed class Quiz(val q: String, val explain: String) {
        class Choice(q: String, val options: List<String>, val answer: Int, explain: String) : Quiz(q, explain)
        class OX(q: String, val answer: Boolean, explain: String) : Quiz(q, explain)
    }

    data class Region(val name: String, val lat: Double, val lng: Double, val keys: List<String>, val qs: List<Quiz>)

    /** 객관식: 첫 번째 인자가 정답. 출제 시 보기 순서를 섞는다 */
    private fun c(q: String, a: String, o1: String, o2: String, explain: String) = Quiz.Choice(q, listOf(a, o1, o2), 0, explain)
    private fun ox(q: String, a: Boolean, explain: String) = Quiz.OX(q, a, explain)

    val regions = listOf(
        Region("천안", 36.815, 127.114, listOf("천안"), listOf(
            ox("천안 독립기념관은 1987년 광복절에 문을 열었다.", true, "정답은 O! 1987년 8월 15일에 개관했어요."),
            c("천안의 대표 간식은 무엇일까요?", "호두과자", "찹쌀떡", "붕어빵", "정답은 호두과자! 휴게소 단골 간식이죠."),
        )),
        Region("대전", 36.350, 127.385, listOf("대전"), listOf(
            c("1993년 대전 엑스포의 마스코트는?", "꿈돌이", "호돌이", "수호랑", "정답은 꿈돌이!"),
            ox("대전 엑스포는 2002년에 열렸다.", false, "정답은 X! 1993년에 열렸어요."),
        )),
        Region("전주", 35.824, 127.148, listOf("전주"), listOf(
            c("전주를 대표하는 음식은?", "비빔밥", "닭갈비", "순두부", "정답은 전주비빔밥!"),
            ox("전주 한옥마을에는 조선 태조의 어진을 모신 경기전이 있다.", true, "정답은 O! 경기전에 태조 어진이 모셔져 있어요."),
        )),
        Region("강릉", 37.752, 128.876, listOf("강릉"), listOf(
            c("율곡 이이가 태어난 강릉의 집은?", "오죽헌", "경복궁", "첨성대", "정답은 오죽헌!"),
            ox("강릉 초당순두부는 바닷물로 간을 해서 만든다.", true, "정답은 O! 바닷물을 간수로 써요."),
        )),
        Region("평창", 37.370, 128.390, listOf("평창"), listOf(
            c("2018 평창 동계올림픽의 마스코트는?", "수호랑", "꿈돌이", "호돌이", "정답은 수호랑!"),
            ox("2018년 동계올림픽은 평창에서 열렸다.", true, "정답은 O!"),
        )),
        Region("경주", 35.856, 129.224, listOf("경주"), listOf(
            c("경주는 어느 나라의 수도였을까요?", "신라", "백제", "고구려", "정답은 신라! 천 년의 수도였죠."),
            ox("불국사는 백제 시대에 지어졌다.", false, "정답은 X! 신라 시대 절이에요."),
        )),
        Region("부산", 35.180, 129.076, listOf("부산"), listOf(
            c("부산의 대표 해수욕장은?", "해운대", "경포대", "대천", "정답은 해운대!"),
            ox("부산은 우리나라에서 두 번째로 인구가 많은 도시다.", true, "정답은 O! 서울 다음이에요."),
        )),
        Region("여수", 34.760, 127.662, listOf("여수"), listOf(
            c("버스커버스커의 노래, 여수 ○○○. 빈칸은?", "밤바다", "갈매기", "등대", "정답은 여수 밤바다!"),
            ox("2012년 세계박람회는 여수에서 열렸다.", true, "정답은 O!"),
        )),
        Region("춘천", 37.881, 127.730, listOf("춘천"), listOf(
            c("춘천의 대표 음식은?", "닭갈비", "비빔밥", "돼지국밥", "정답은 춘천 닭갈비!"),
            ox("춘천은 강원특별자치도의 도청 소재지다.", true, "정답은 O!"),
        )),
        Region("수원", 37.263, 127.029, listOf("수원"), listOf(
            c("정조가 쌓은 수원의 성곽은?", "화성", "남한산성", "북한산성", "정답은 수원 화성!"),
            ox("수원 화성은 유네스코 세계유산이다.", true, "정답은 O! 1997년에 등재됐어요."),
        )),
        Region("안동", 36.568, 128.729, listOf("안동"), listOf(
            c("안동의 유네스코 세계유산 마을은?", "하회마을", "한옥마을", "민속촌", "정답은 하회마을!"),
            ox("안동 하회마을에는 하회탈이 전해 내려온다.", true, "정답은 O!"),
        )),
        Region("서울", 37.566, 126.978, listOf("서울"), listOf(
            c("광화문 뒤에 있는 조선의 으뜸 궁궐은?", "경복궁", "덕수궁", "창덕궁", "정답은 경복궁!"),
        )),
        Region("광주", 35.160, 126.851, listOf("광주광역시", "광주 "), listOf(
            c("광주를 대표하는 산은?", "무등산", "한라산", "설악산", "정답은 무등산!"),
            ox("광주광역시는 전라남도 도청 소재지다.", false, "정답은 X! 전남도청은 무안에 있어요."),
        )),
        Region("통영", 34.854, 128.433, listOf("통영"), listOf(
            c("통영에서 시작된 김밥은?", "충무김밥", "마약김밥", "꼬마김밥", "정답은 충무김밥!"),
        )),
        Region("담양", 35.321, 126.988, listOf("담양"), listOf(
            c("담양의 대나무 숲 정원은?", "죽녹원", "오죽헌", "하회마을", "정답은 죽녹원!"),
        )),
    )

    /** 위치를 모를 때 쓰는 전국 퀴즈 */
    val nationwide = listOf(
        ox("우리나라에서 가장 높은 산은 한라산이다.", true, "정답은 O!"),
        c("우리나라에서 가장 긴 강은?", "낙동강", "한강", "금강", "정답은 낙동강!"),
        ox("독도는 경상북도 울릉군에 속한다.", true, "정답은 O!"),
        c("경부고속도로는 서울과 어디를 잇는 길일까요?", "부산", "광주", "강릉", "정답은 부산!"),
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
