/**
 * Voice Assistant for IoT Dashboard
 * Tích hợp Web Speech API + Gemini AI để điều khiển dashboard bằng giọng nói
 */

class VoiceAssistant {
    constructor() {
        this.isListening = false;
        this.recognition = null;
        this.synthesis = window.speechSynthesis;
        this.apiKey = null; // Sẽ set từ UI
        this.geminiEndpoint = '/api/voice-assistant/gemini'; // Backend proxy
        
        // Rate limiting
        this.lastRequestTime = 0;
        this.minRequestInterval = 2000; // 2 giây giữa các requests
        this.requestCount = 0;
        this.requestResetTime = Date.now() + 60000; // Reset sau 1 phút
        this.maxRequestsPerMinute = 10; // Giới hạn 10 requests/phút
        
        this.initSpeechRecognition();
        this.initUI();
    }

    initSpeechRecognition() {
        // Check browser support
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        
        if (!SpeechRecognition) {
            console.error('⚠️ Browser không hỗ trợ Web Speech API');
            return;
        }

        this.recognition = new SpeechRecognition();
        this.recognition.lang = 'vi-VN'; // Tiếng Việt
        this.recognition.continuous = false;
        this.recognition.interimResults = false;
        this.recognition.maxAlternatives = 1;

        this.recognition.onstart = () => {
            this.isListening = true;
            this.updateUI('listening');
            console.log('🎤 Đang lắng nghe...');
        };

        this.recognition.onresult = (event) => {
            const transcript = event.results[0][0].transcript;
            console.log('📝 Nhận diện: ' + transcript);
            this.updateUI('processing', transcript);
            this.processCommand(transcript);
        };

        this.recognition.onerror = (event) => {
            console.error('❌ Lỗi nhận diện giọng nói:', event.error);
            this.updateUI('error', event.error);
            this.isListening = false;
        };

        this.recognition.onend = () => {
            this.isListening = false;
            console.log('🛑 Kết thúc lắng nghe');
        };
    }

    initUI() {
        // Create voice assistant UI
        const assistantHTML = `
            <div id="voice-assistant-container" style="position: fixed; bottom: 20px; right: 20px; z-index: 9999;">
                <!-- Floating Button -->
                <button id="voice-btn" class="voice-button" title="Voice Assistant (Alt+V)">
                    <i class="fas fa-microphone"></i>
                </button>

                <!-- Assistant Panel -->
                <div id="voice-panel" class="voice-panel" style="display: none;">
                    <div class="voice-header">
                        <h4>🎤 Voice Assistant</h4>
                        <button id="close-voice-panel" class="close-btn">&times;</button>
                    </div>

                    <!-- API Key Input -->
                    <div class="voice-section" id="api-key-section">
                        <label>Google AI API Key:</label>
                        <input type="password" id="gemini-api-key" placeholder="Nhập API key của bạn">
                        <button id="save-api-key" class="btn-primary">Lưu</button>
                        <small><a href="https://ai.google.dev/gemini-api/docs/api-key" target="_blank">Lấy API key miễn phí</a></small>
                    </div>

                    <!-- Status Display -->
                    <div class="voice-section">
                        <div id="voice-status" class="voice-status">
                            <div class="status-icon">💬</div>
                            <div class="status-text">Nhấn mic để bắt đầu</div>
                        </div>
                    </div>

                    <!-- Transcript Display -->
                    <div class="voice-section">
                        <div id="voice-transcript" class="voice-transcript">
                            <strong>Bạn:</strong> <span id="user-text">...</span>
                        </div>
                        <div id="voice-response" class="voice-response">
                            <strong>AI:</strong> <span id="ai-text">...</span>
                        </div>
                    </div>

                    <!-- Quick Commands -->
                    <div class="voice-section">
                        <small><strong>Thử các lệnh:</strong></small>
                        <div class="quick-commands">
                            <button class="quick-cmd" data-cmd="Có bao nhiêu công nhân đang online?">👷 Số công nhân</button>
                            <button class="quick-cmd" data-cmd="Có cảnh báo nguy hiểm nào không?">⚠️ Cảnh báo</button>
                            <button class="quick-cmd" data-cmd="Hiển thị vị trí công nhân trên bản đồ">📍 Bản đồ</button>
                            <button class="quick-cmd" data-cmd="Cho tôi xem tổng quan dashboard">📊 Tổng quan</button>
                        </div>
                    </div>
                </div>
            </div>

            <style>
                .voice-button {
                    width: 60px;
                    height: 60px;
                    border-radius: 50%;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    border: none;
                    color: white;
                    font-size: 24px;
                    cursor: pointer;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                    transition: all 0.3s ease;
                }

                .voice-button:hover {
                    transform: scale(1.1);
                    box-shadow: 0 6px 16px rgba(0,0,0,0.4);
                }

                .voice-button.listening {
                    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                    animation: pulse 1.5s infinite;
                }

                @keyframes pulse {
                    0%, 100% { transform: scale(1); }
                    50% { transform: scale(1.1); }
                }

                .voice-panel {
                    position: absolute;
                    bottom: 70px;
                    right: 0;
                    width: 400px;
                    max-height: 600px;
                    background: white;
                    border-radius: 16px;
                    box-shadow: 0 8px 32px rgba(0,0,0,0.2);
                    overflow: hidden;
                    animation: slideUp 0.3s ease;
                }

                @keyframes slideUp {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }

                .voice-header {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 15px 20px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                .voice-header h4 {
                    margin: 0;
                    font-size: 18px;
                }

                .close-btn {
                    background: none;
                    border: none;
                    color: white;
                    font-size: 28px;
                    cursor: pointer;
                    line-height: 1;
                }

                .voice-section {
                    padding: 15px 20px;
                    border-bottom: 1px solid #eee;
                }

                .voice-section:last-child {
                    border-bottom: none;
                }

                .voice-section label {
                    display: block;
                    margin-bottom: 8px;
                    font-weight: 500;
                    color: #333;
                }

                .voice-section input {
                    width: 100%;
                    padding: 10px;
                    border: 2px solid #ddd;
                    border-radius: 8px;
                    margin-bottom: 8px;
                    font-size: 14px;
                }

                .btn-primary {
                    background: #667eea;
                    color: white;
                    border: none;
                    padding: 10px 20px;
                    border-radius: 8px;
                    cursor: pointer;
                    font-size: 14px;
                    width: 100%;
                }

                .btn-primary:hover {
                    background: #5568d3;
                }

                .voice-status {
                    text-align: center;
                    padding: 20px;
                    background: #f8f9ff;
                    border-radius: 8px;
                }

                .status-icon {
                    font-size: 48px;
                    margin-bottom: 10px;
                }

                .status-text {
                    font-size: 16px;
                    color: #666;
                }

                .voice-transcript, .voice-response {
                    padding: 12px;
                    background: #f5f5f5;
                    border-radius: 8px;
                    margin-bottom: 10px;
                    font-size: 14px;
                    line-height: 1.5;
                }

                .voice-response {
                    background: #e3f2fd;
                }

                .quick-commands {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 8px;
                    margin-top: 8px;
                }

                .quick-cmd {
                    padding: 8px 12px;
                    background: #f0f0f0;
                    border: 1px solid #ddd;
                    border-radius: 6px;
                    cursor: pointer;
                    font-size: 12px;
                    transition: all 0.2s;
                }

                .quick-cmd:hover {
                    background: #667eea;
                    color: white;
                    border-color: #667eea;
                }

                @media (max-width: 768px) {
                    .voice-panel {
                        width: 90vw;
                        right: 5vw;
                    }
                }
            </style>
        `;

        document.body.insertAdjacentHTML('beforeend', assistantHTML);

        // Event listeners
        document.getElementById('voice-btn').addEventListener('click', () => this.toggleListening());
        document.getElementById('close-voice-panel').addEventListener('click', () => this.closePanel());
        document.getElementById('save-api-key').addEventListener('click', () => this.saveApiKey());
        
        // Quick commands
        document.querySelectorAll('.quick-cmd').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const cmd = e.target.getAttribute('data-cmd');
                this.processCommand(cmd);
            });
        });

        // Keyboard shortcut: Alt+V
        document.addEventListener('keydown', (e) => {
            if (e.altKey && e.key === 'v') {
                this.toggleListening();
            }
        });

        // Load saved API key
        this.loadApiKey();
    }

    toggleListening() {
        if (!this.apiKey) {
            this.openPanel();
            alert('⚠️ Vui lòng nhập Google AI API Key trước!');
            return;
        }

        if (this.isListening) {
            this.recognition.stop();
        } else {
            this.openPanel();
            this.recognition.start();
        }
    }

    openPanel() {
        document.getElementById('voice-panel').style.display = 'block';
    }

    closePanel() {
        document.getElementById('voice-panel').style.display = 'none';
    }

    saveApiKey() {
        const key = document.getElementById('gemini-api-key').value.trim();
        if (!key) {
            alert('⚠️ Vui lòng nhập API key!');
            return;
        }

        this.apiKey = key;
        localStorage.setItem('gemini_api_key', key);
        document.getElementById('api-key-section').style.display = 'none';
        this.updateUI('ready', 'API key đã được lưu! ✅');
    }

    loadApiKey() {
        const savedKey = localStorage.getItem('gemini_api_key');
        if (savedKey) {
            this.apiKey = savedKey;
            document.getElementById('gemini-api-key').value = savedKey;
            document.getElementById('api-key-section').style.display = 'none';
        }
    }

    updateUI(state, message = '') {
        const statusIcon = document.querySelector('.status-icon');
        const statusText = document.querySelector('.status-text');
        const voiceBtn = document.getElementById('voice-btn');

        switch(state) {
            case 'listening':
                statusIcon.textContent = '🎤';
                statusText.textContent = 'Đang lắng nghe...';
                voiceBtn.classList.add('listening');
                break;
            case 'processing':
                statusIcon.textContent = '⏳';
                statusText.textContent = 'Đang xử lý...';
                voiceBtn.classList.remove('listening');
                document.getElementById('user-text').textContent = message;
                break;
            case 'speaking':
                statusIcon.textContent = '🔊';
                statusText.textContent = 'Đang trả lời...';
                break;
            case 'ready':
                statusIcon.textContent = '✅';
                statusText.textContent = message || 'Sẵn sàng!';
                voiceBtn.classList.remove('listening');
                break;
            case 'error':
                statusIcon.textContent = '❌';
                statusText.textContent = 'Lỗi: ' + message;
                voiceBtn.classList.remove('listening');
                break;
        }
    }

    async processCommand(command) {
        try {
            // Check rate limit
            const now = Date.now();
            
            // Reset counter mỗi phút
            if (now > this.requestResetTime) {
                this.requestCount = 0;
                this.requestResetTime = now + 60000;
            }
            
            // Kiểm tra số lượng requests
            if (this.requestCount >= this.maxRequestsPerMinute) {
                const waitTime = Math.ceil((this.requestResetTime - now) / 1000);
                throw new Error(`Vượt quá giới hạn ${this.maxRequestsPerMinute} requests/phút. Vui lòng đợi ${waitTime} giây.`);
            }
            
            // Kiểm tra thời gian chờ giữa requests
            const timeSinceLastRequest = now - this.lastRequestTime;
            if (timeSinceLastRequest < this.minRequestInterval) {
                const waitTime = Math.ceil((this.minRequestInterval - timeSinceLastRequest) / 1000);
                throw new Error(`Vui lòng đợi ${waitTime} giây trước khi hỏi tiếp.`);
            }
            
            // Hiển thị command
            document.getElementById('user-text').textContent = command;
            
            // Update request tracking
            this.lastRequestTime = now;
            this.requestCount++;
            
            // Gọi Gemini API với tools
            const response = await this.callGeminiWithTools(command);
            
            // Hiển thị response
            document.getElementById('ai-text').textContent = response;
            
            // Đọc response bằng giọng nói
            this.speak(response);
            
            this.updateUI('ready', 'Hoàn thành!');
        } catch (error) {
            console.error('❌ Lỗi xử lý:', error);
            const errorMsg = 'Xin lỗi, đã có lỗi xảy ra: ' + error.message;
            document.getElementById('ai-text').textContent = errorMsg;
            this.updateUI('error', error.message);
        }
    }

    async callGeminiWithTools(userQuery) {
        // Định nghĩa tools (functions) cho Gemini
        const tools = [
            {
                functionDeclarations: [
                    {
                        name: 'get_workers',
                        description: 'Lấy danh sách tất cả công nhân và trạng thái online/offline',
                        parameters: {
                            type: 'object',
                            properties: {}
                        }
                    },
                    {
                        name: 'get_recent_alerts',
                        description: 'Lấy danh sách cảnh báo nguy hiểm gần đây (FALL, HELP_REQUEST)',
                        parameters: {
                            type: 'object',
                            properties: {
                                limit: {
                                    type: 'integer',
                                    description: 'Số lượng cảnh báo cần lấy (mặc định 10)'
                                }
                            }
                        }
                    },
                    {
                        name: 'get_helmet_status',
                        description: 'Kiểm tra trạng thái chi tiết của một mũ bảo hộ (pin, vị trí, online/offline)',
                        parameters: {
                            type: 'object',
                            properties: {
                                mac_address: {
                                    type: 'string',
                                    description: 'Địa chỉ MAC của mũ bảo hộ (vd: F4DD40BA2010)'
                                }
                            },
                            required: ['mac_address']
                        }
                    },
                    {
                        name: 'get_map_data',
                        description: 'Lấy vị trí hiện tại của tất cả công nhân trên bản đồ',
                        parameters: {
                            type: 'object',
                            properties: {}
                        }
                    },
                    {
                        name: 'get_dashboard_overview',
                        description: 'Lấy tổng quan dashboard (tổng số công nhân, số active, số alerts, hiệu suất)',
                        parameters: {
                            type: 'object',
                            properties: {}
                        }
                    }
                ]
            }
        ];

        // Gọi Gemini API qua backend proxy
        const geminiResponse = await fetch(this.geminiEndpoint, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-API-Key': this.apiKey
            },
            body: JSON.stringify({
                contents: [{
                    role: 'user',
                    parts: [{ text: userQuery }]
                }],
                tools: tools,
                systemInstruction: {
                    parts: [{
                        text: `Bạn là trợ lý AI cho hệ thống giám sát an toàn công nhân xây dựng.
                        Luôn trả lời bằng tiếng Việt, ngắn gọn, dễ hiểu.
                        Sử dụng các function tools để lấy dữ liệu realtime từ backend.
                        Ưu tiên thông tin về an toàn và cảnh báo.`
                    }]
                }
            })
        });

        if (!geminiResponse.ok) {
            if (geminiResponse.status === 429) {
                throw new Error('Vượt quá giới hạn API của Google (15 requests/phút). Vui lòng đợi 1 phút hoặc nâng cấp lên paid tier.');
            } else if (geminiResponse.status === 401) {
                throw new Error('API key không hợp lệ. Vui lòng kiểm tra lại.');
            } else if (geminiResponse.status === 403) {
                throw new Error('API key bị từ chối. Vui lòng tạo key mới.');
            }
            const errorText = await geminiResponse.text();
            console.error('❌ Gemini initial error response:', errorText);
            throw new Error(`Gemini API error: ${geminiResponse.status}`);
        }

        const data = await geminiResponse.json();
        console.log('📥 Initial Gemini response:', data);
        
        // Validate response structure
        if (!data.candidates || data.candidates.length === 0) {
            console.error('❌ Invalid initial response:', data);
            throw new Error('Gemini API trả về response không hợp lệ');
        }
        
        const candidate = data.candidates[0];
        
        // Check if response is blocked
        if (!candidate.content) {
            console.error('❌ Response blocked or missing content:', candidate);
            const reason = candidate.finishReason || 'UNKNOWN';
            throw new Error(`Gemini blocked response: ${reason}`);
        }
        
        if (!candidate.content.parts || candidate.content.parts.length === 0) {
            console.error('❌ Missing parts in response:', candidate.content);
            throw new Error('Gemini API không trả về nội dung');
        }
        
        // Check if Gemini wants to call a function
        if (candidate.content.parts[0].functionCall) {
            const functionCall = candidate.content.parts[0].functionCall;
            const functionName = functionCall.name;
            const functionArgs = functionCall.args || {};

            console.log('🔧 Calling function:', functionName, functionArgs);

            // Execute function
            const functionResult = await this.executeFunction(functionName, functionArgs);
            console.log('📥 Function result:', functionResult);

            // Send function result back to Gemini
            const finalResponse = await fetch(this.geminiEndpoint, {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-API-Key': this.apiKey
                },
                body: JSON.stringify({
                    contents: [
                        {
                            role: 'user',
                            parts: [{ text: userQuery }]
                        },
                        {
                            role: 'model',
                            parts: [{ functionCall: functionCall }]
                        },
                        {
                            role: 'function',
                            parts: [{
                                functionResponse: {
                                    name: functionName,
                                    response: {
                                        result: functionResult
                                    }
                                }
                            }]
                        }
                    ]
                })
            });

            if (!finalResponse.ok) {
                if (finalResponse.status === 429) {
                    throw new Error('Vượt quá giới hạn API (15 requests/phút). Đợi 1 phút hoặc nâng cấp paid tier.');
                }
                const errorText = await finalResponse.text();
                console.error('❌ Gemini API error response:', errorText);
                throw new Error(`Gemini API error: ${finalResponse.status}`);
            }

            const finalData = await finalResponse.json();
            console.log('📥 Final Gemini response:', finalData);
            
            // Validate response structure
            if (!finalData.candidates || finalData.candidates.length === 0) {
                console.error('❌ Invalid response structure:', finalData);
                throw new Error('Gemini API trả về response không hợp lệ');
            }
            
            if (!finalData.candidates[0].content || !finalData.candidates[0].content.parts || 
                finalData.candidates[0].content.parts.length === 0) {
                console.error('❌ Missing content in response:', finalData.candidates[0]);
                throw new Error('Gemini API không trả về nội dung');
            }
            
            return finalData.candidates[0].content.parts[0].text;
        } else {
            // Direct text response
            return candidate.content.parts[0].text;
        }
    }

    async executeFunction(name, args) {
        const baseUrl = window.location.origin;
        
        console.log(`📞 Executing backend API: ${name}`, args);

        switch(name) {
            case 'get_workers':
                // Sử dụng existing API
                return await this.apiCall(`${baseUrl}/api/workers`);
            
            case 'get_recent_alerts':
                const limit = args.limit || 10;
                return await this.apiCall(`${baseUrl}/api/dashboard/alerts/recent?limit=${limit}`);
            
            case 'get_helmet_status':
                const macAddress = args.mac_address;
                if (!macAddress) {
                    return { error: 'MAC address required' };
                }
                // Get map data and filter by MAC
                const mapData = await this.apiCall(`${baseUrl}/api/positioning/tags`);
                const helmet = mapData.find(h => h.macAddress === macAddress);
                if (!helmet) {
                    return { error: `Helmet ${macAddress} not found or offline` };
                }
                return helmet;
            
            case 'get_map_data':
                return await this.apiCall(`${baseUrl}/api/positioning/tags`);
            
            case 'get_dashboard_overview':
                return await this.apiCall(`${baseUrl}/api/dashboard/overview`);
            
            default:
                return { error: 'Unknown function: ' + name };
        }
    }

    async apiCall(url) {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                return { error: `API error: ${response.status}` };
            }
            return await response.json();
        } catch (error) {
            return { error: error.message };
        }
    }

    speak(text) {
        // Cancel any ongoing speech
        this.synthesis.cancel();

        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'vi-VN';
        utterance.rate = 1.0;
        utterance.pitch = 1.0;

        // Select Vietnamese voice if available
        const voices = this.synthesis.getVoices();
        const vietnameseVoice = voices.find(voice => 
            voice.lang === 'vi-VN' || 
            voice.lang.startsWith('vi')
        );
        if (vietnameseVoice) {
            utterance.voice = vietnameseVoice;
        }

        utterance.onstart = () => {
            this.updateUI('speaking');
        };

        utterance.onend = () => {
            this.updateUI('ready');
        };

        this.synthesis.speak(utterance);
    }
}

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.voiceAssistant = new VoiceAssistant();
    });
} else {
    window.voiceAssistant = new VoiceAssistant();
}
