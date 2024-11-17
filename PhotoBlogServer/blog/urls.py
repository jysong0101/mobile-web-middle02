from django.urls import path, include
from rest_framework import routers
from . import views

router = routers.DefaultRouter()
router.register('Post', views.BlogImages)  # BlogImages 뷰셋을 등록

urlpatterns = [
    path('', views.post_list, name='post_list'),  # 기존 메인 페이지 뷰
    path('post/<int:pk>/', views.post_detail, name='post_detail'),
    path('post/new/', views.post_new, name='post_new'),
    path('post/<int:pk>/edit/', views.post_edit, name='post_edit'),
    path('js_test/', views.js_test, name='js_test'),
    path('api_root/', include(router.urls)),  # API root 경로 추가
]
