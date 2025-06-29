package com.gco.stt.controller;

import com.google.cloud.speech.v2.RecognizeResponse;
import com.google.cloud.speech.v2.SpeechClient;
import com.google.cloud.speech.v2.SpeechRecognitionAlternative;
import com.google.cloud.speech.v2.SpeechRecognitionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpeechRecorderController.class)
class SpeechRecorderControllerMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpeechClient speechClient;

    @Test
    void uploadAudioFile_WithValidFile_ShouldReturnTranscript() throws Exception {
        // Mock 응답 생성
        SpeechRecognitionAlternative alternative = SpeechRecognitionAlternative.newBuilder()
                .setTranscript("안녕하세요 테스트입니다")
                .build();
        
        SpeechRecognitionResult result = SpeechRecognitionResult.newBuilder()
                .addAlternatives(alternative)
                .build();
        
        RecognizeResponse mockResponse = RecognizeResponse.newBuilder()
                .addResults(result)
                .build();

        when(speechClient.recognize(any())).thenReturn(mockResponse);

        MockMultipartFile audioFile = new MockMultipartFile(
                "audio",
                "test-audio.wav",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "test audio content".getBytes()
        );

        mockMvc.perform(multipart("/api/speech/upload")
                        .file(audioFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("음성 변환 성공!"))
                .andExpect(jsonPath("$.transcript").value("안녕하세요 테스트입니다"));
    }

    @Test
    void uploadAudioFile_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "audio",
                "empty.wav",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/speech/upload")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("업로드된 파일이 없음"));
    }

    @Test
    void uploadAudioFile_WithNoResults_ShouldReturnNoSpeechRecognized() throws Exception {
        // 결과가 없는 응답
        RecognizeResponse emptyResponse = RecognizeResponse.newBuilder().build();
        when(speechClient.recognize(any())).thenReturn(emptyResponse);

        MockMultipartFile audioFile = new MockMultipartFile(
                "audio",
                "test-audio.wav",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "test audio content".getBytes()
        );

        mockMvc.perform(multipart("/api/speech/upload")
                        .file(audioFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("음성 인식 불가"));
    }
}