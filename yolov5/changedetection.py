import os
import cv2
import pathlib
import requests
from datetime import datetime

class ChangeDetection:
    result_prev = []
    HOST = 'https://juyeop.pythonanywhere.com/'
    username = 'admin'
    password = 'asns7873'
    token = '5f16ab618d07687de55f0df65fc5c9ff5838683e'
    title = ''
    text = ''

    def __init__(self, names):
        self.result_prev = [0 for _ in range(len(names))]

        # res = requests.post(self.HOST + '/api-token-auth/', {
        #     'username': self.username,
        #     'password': self.password,
        # })
        # res.raise_for_status()
        # self.token = res.json()['token']  # 토큰 저장
        # print(self.token)

    def add(self, names, detected_current, save_dir, image):
        self.title = ''
        self.text = ''
        change_flag = 0  # 변화 감지 플래그
        i = 0
        while i < len(self.result_prev):
            if self.result_prev[i] == 0 and detected_current[i] == 1:
                change_flag = 1
                self.title = names[i]
                self.text += names[i] + ','
            i += 1

        self.result_prev = detected_current[:]  # 객체 검출 상태 저장

        if change_flag == 1:
            self.send(save_dir, image)

    def send(self, save_dir, image):
        now = datetime.now()
        
        # save_dir을 문자열로 변환
        save_path = os.path.join(os.getcwd(), str(save_dir), 'detected', str(now.year), str(now.month), str(now.day))
        
        # 디렉토리 생성
        pathlib.Path(save_path).mkdir(parents=True, exist_ok=True)

        # 이미지 저장 경로 생성
        full_path = os.path.join(save_path, '{0}-{1}-{2}-{3}.jpg'.format(now.hour, now.minute, now.second, now.microsecond))

        # 이미지 크기 조정 및 저장
        dst = cv2.resize(image, dsize=(320, 240), interpolation=cv2.INTER_AREA)
        cv2.imwrite(full_path, dst)

        # 인증이 필요한 요청에 아래의 headers를 붙임
        headers = {'Authorization': 'JWT ' + self.token, 'Accept': 'application/json'}

        # Post Create
        data = {
            'title': self.title or "Default Title",
            'text': self.text or "Default Text",
            'created_date': now.isoformat(),
            'published_date': None,
            'author': 1
        }
        file = {'image': open(full_path, 'rb')}
        
        # 서버 요청 및 결과 출력
        res = requests.post(self.HOST + '/api_root/Post/', data=data, files=file, headers=headers)
        print(f"Response Status Code: {res.status_code}")
        print(f"Response Text: {res.text}")
