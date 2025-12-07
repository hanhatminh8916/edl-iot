/**
 * Voice Assistant for IoT Dashboard
 * Tích hợp Web Speech API + Gemini AI để điều khiển dashboard bằng giọng nói
 */

class VoiceAssistant {
    constructor() {
        this.isListening = false;
        this.isSpeaking = false; // Track if TTS is active
        this.recognition = null;
        this.synthesis = window.speechSynthesis;
        this.apiKey = null; // Không cần cho LM Studio
        this.llmEndpoint = '/api/voice-assistant/lmstudio'; // LM Studio proxy
        this.pendingNavigation = null; // Store navigation to execute after response
        this.vietnameseVoice = null; // Cache Vietnamese voice
        
        // ElevenLabs TTS
        this.elevenLabsApiKey = 'sk_701323a10ba0aade7e19a0ec0ee2c79148381bc038cc874c';
        this.elevenLabsVoiceId = 'iSFxP4Z6YNcx9OXl62Ic'; // Adam voice (supports multilingual)
        this.useElevenLabs = true; // Enable ElevenLabs by default
        
        // Always Listening mode
        this.alwaysListening = localStorage.getItem('voice_always_listening') === 'true';
        this.isWelcomePlayed = false;
        
        // Rate limiting
        this.lastRequestTime = 0;
        this.minRequestInterval = 2000; // 2 giây giữa các requests
        this.requestCount = 0;
        this.requestResetTime = Date.now() + 60000; // Reset sau 1 phút
        this.maxRequestsPerMinute = 10; // Giới hạn 10 requests/phút
        
        this.initSpeechRecognition();
        this.initUI();
        this.loadVietnameseVoice(); // Load Vietnamese voice as fallback
        
        // Auto-start if always listening is enabled
        setTimeout(() => {
            if (this.alwaysListening) {
                this.playWelcomeAndStart();
            }
        }, 1000); // Wait 1s for page to load
    }

    playWelcomeAndStart() {
        if (!this.isWelcomePlayed) {
            this.isWelcomePlayed = true;
            this.speak('EDL Assistant đã sẵn sàng');
            
            // Start listening after welcome message
            setTimeout(() => {
                this.startContinuousListening();
            }, 2500);
        }
    }

    startContinuousListening() {
        if (!this.isListening && this.alwaysListening) {
            console.log('🎤 Starting continuous listening mode');
            this.recognition.start();
        }
    }

    loadVietnameseVoice() {
        // Voices need to be loaded asynchronously
        const loadVoices = () => {
            const voices = this.synthesis.getVoices();
            console.log('🔊 Available voices:', voices.length);
            
            // Find Vietnamese voice
            this.vietnameseVoice = voices.find(voice => 
                voice.lang === 'vi-VN' || 
                voice.lang.startsWith('vi')
            );
            
            if (this.vietnameseVoice) {
                console.log('✅ Vietnamese voice found:', this.vietnameseVoice.name);
            } else {
                console.warn('⚠️ No Vietnamese voice found, using default');
                // Find any Google voice as fallback
                this.vietnameseVoice = voices.find(voice => 
                    voice.name.includes('Google')
                ) || voices[0];
            }
        };

        // Load voices immediately
        loadVoices();

        // Also listen for voiceschanged event (for Chrome)
        if (this.synthesis.onvoiceschanged !== undefined) {
            this.synthesis.onvoiceschanged = loadVoices;
        }
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
            
            // Only auto-restart if always listening AND not currently speaking
            if (this.alwaysListening && !this.isSpeaking) {
                setTimeout(() => {
                    this.startContinuousListening();
                }, 500); // Small delay before restart
            }
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

                    <!-- API Key Input - Hidden for LM Studio -->
                    <div class="voice-section" id="api-key-section" style="display: none;">
                        <label>API Key:</label>
                        <input type="password" id="gemini-api-key" placeholder="Không cần cho LM Studio">
                        <button id="save-api-key" class="btn-primary">Lưu</button>
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
                            <button class="quick-cmd" data-cmd="Cho tôi xem dashboard">🏠 Dashboard</button>
                            <button class="quick-cmd" data-cmd="Hiển thị bản đồ vị trí">📍 Bản đồ</button>
                            <button class="quick-cmd" data-cmd="Có cảnh báo nguy hiểm nào không?">⚠️ Cảnh báo</button>
                            <button class="quick-cmd" data-cmd="Có bao nhiêu công nhân đang online?">👷 Công nhân</button>
                        </div>
                    </div>

                    <!-- Settings Section -->
                    <div class="voice-section" style="background: #f8f9fa;">
                        <small><strong>⚙️ Cài đặt:</strong></small>
                        <div style="margin-top: 10px; display: flex; align-items: center; justify-content: space-between;">
                            <label style="margin: 0; cursor: pointer; display: flex; align-items: center;">
                                <span style="margin-right: 10px;">🎤 Always Listening</span>
                            </label>
                            <label class="toggle-switch">
                                <input type="checkbox" id="always-listening-toggle" ${this.alwaysListening ? 'checked' : ''}>
                                <span class="toggle-slider"></span>
                            </label>
                        </div>
                        <small style="color: #666; display: block; margin-top: 5px;">Tự động lắng nghe khi vào trang</small>
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

                /* Toggle Switch */
                .toggle-switch {
                    position: relative;
                    display: inline-block;
                    width: 50px;
                    height: 24px;
                }

                .toggle-switch input {
                    opacity: 0;
                    width: 0;
                    height: 0;
                }

                .toggle-slider {
                    position: absolute;
                    cursor: pointer;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background-color: #ccc;
                    transition: 0.4s;
                    border-radius: 24px;
                }

                .toggle-slider:before {
                    position: absolute;
                    content: "";
                    height: 18px;
                    width: 18px;
                    left: 3px;
                    bottom: 3px;
                    background-color: white;
                    transition: 0.4s;
                    border-radius: 50%;
                }

                .toggle-switch input:checked + .toggle-slider {
                    background-color: #667eea;
                }

                .toggle-switch input:checked + .toggle-slider:before {
                    transform: translateX(26px);
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
        
        // Always Listening toggle
        document.getElementById('always-listening-toggle').addEventListener('change', (e) => {
            this.alwaysListening = e.target.checked;
            localStorage.setItem('voice_always_listening', this.alwaysListening);
            
            if (this.alwaysListening) {
                console.log('✅ Always Listening enabled');
                this.playWelcomeAndStart();
            } else {
                console.log('❌ Always Listening disabled');
                if (this.isListening) {
                    this.recognition.stop();
                }
            }
        });
        
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
        // LM Studio không cần API key
        // Hide API key section completely
        const apiKeySection = document.getElementById('api-key-section');
        if (apiKeySection) {
            apiKeySection.style.display = 'none';
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
            
            // Check if we have pending navigation
            const hasPendingNav = !!this.pendingNavigation;
            console.log('🔍 Has pending navigation:', hasPendingNav, this.pendingNavigation);
            
            // Đọc response bằng giọng nói
            this.speak(response, hasPendingNav);
            
            this.updateUI('ready', 'Hoàn thành!');
            
            // Execute pending navigation AFTER showing response
            if (this.pendingNavigation) {
                const navFunction = this.pendingNavigation;
                this.pendingNavigation = null;
                
                console.log('⏰ Navigation will execute in 2 seconds...', navFunction);
                
                // Wait 2 seconds to let user see/hear the response
                setTimeout(() => {
                    console.log('🚀 Executing navigation now!', navFunction);
                    this.executeNavigation(navFunction.function, navFunction.args || {});
                }, 2000);
            }
        } catch (error) {
            console.error('❌ Lỗi xử lý:', error);
            const errorMsg = 'Xin lỗi, đã có lỗi xảy ra: ' + error.message;
            document.getElementById('ai-text').textContent = errorMsg;
            this.updateUI('error', error.message);
        }
    }

    async callGeminiWithTools(userQuery) {
        // Load system prompt from file
        let systemPrompt = '';
        try {
            const promptResponse = await fetch('/voice-assistant-prompt.md');
            if (promptResponse.ok) {
                systemPrompt = await promptResponse.text();
            }
        } catch (error) {
            console.warn('⚠️ Could not load system prompt from file, using fallback');
            systemPrompt = `Bạn là trợ lý AI cho hệ thống giám sát an toàn công nhân xây dựng.
Khi user yêu cầu chuyển trang/xem dữ liệu, trả về JSON {"function": "tên_function", "args": {}}.
Các function: navigate_to_dashboard, navigate_to_positioning, navigate_to_alerts, navigate_to_employees, get_workers, get_recent_alerts, get_dashboard_overview, read_dashboard_stats.`;
        }

        // Gọi LM Studio API (OpenAI-compatible format)
        const llmResponse = await fetch(this.llmEndpoint, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                model: "local-model", // LM Studio sẽ dùng model đang load
                messages: [
                    {
                        role: "system",
                        content: systemPrompt
                    },
                    {
                        role: "user",
                        content: userQuery
                    }
                ],
                temperature: 0.3,
                max_tokens: 150,
                stream: false
            })
        });

        if (!llmResponse.ok) {
            const errorText = await llmResponse.text();
            console.error('❌ LM Studio error response:', errorText);
            throw new Error(`LM Studio API error: ${llmResponse.status}`);
        }

        const data = await llmResponse.json();
        console.log('📥 LM Studio response:', data);
        
        // OpenAI format: data.choices[0].message.content
        if (!data.choices || data.choices.length === 0) {
            console.error('❌ Invalid response structure:', data);
            throw new Error('LM Studio trả về response không hợp lệ');
        }

        const responseText = data.choices[0].message.content;
        console.log('💬 LM response:', responseText);

        // Check if LLM wants to call a function (simple JSON detection)
        try {
            const jsonMatch = responseText.match(/\{[\s\S]*"function"[\s\S]*\}/);
            if (jsonMatch) {
                const functionCall = JSON.parse(jsonMatch[0]);
                console.log('🔧 Detected function call:', functionCall);
                
                // Check if this is a navigation function - handle specially
                const isNavigation = functionCall.function.startsWith('navigate_to_');
                const isReadStats = functionCall.function === 'read_dashboard_stats';
                console.log('🧭 Is navigation function?', isNavigation, functionCall.function);
                console.log('📊 Is read stats?', isReadStats);
                
                if (isNavigation) {
                    // For navigation, just return the message and navigate AFTER response
                    const navMessages = {
                        'navigate_to_dashboard': 'Đang chuyển sang trang Dashboard...',
                        'navigate_to_positioning': 'Đang chuyển sang trang Giám sát vị trí...',
                        'navigate_to_alerts': 'Đang chuyển sang trang Cảnh báo...',
                        'navigate_to_employees': 'Đang chuyển sang trang Quản lý nhân viên...'
                    };
                    
                    // Store navigation info to execute after response
                    this.pendingNavigation = functionCall;
                    console.log('💾 Stored pending navigation:', this.pendingNavigation);
                    
                    return navMessages[functionCall.function] || 'Đang chuyển trang...';
                }
                
                if (isReadStats) {
                    // For read_dashboard_stats, execute immediately and return the message
                    const statsResult = await this.executeFunction(functionCall.function, functionCall.args || {});
                    console.log('📊 Stats result:', statsResult);
                    
                    if (statsResult.error) {
                        return 'Không thể đọc thống kê: ' + statsResult.error;
                    }
                    
                    return statsResult.message || 'Đã hiển thị thống kê dashboard.';
                }
                
                // Execute non-navigation function
                const functionResult = await this.executeFunction(functionCall.function, functionCall.args || {});
                console.log('📥 Function result:', functionResult);

                // Send result back to LLM for natural language response
                const finalResponse = await fetch(this.llmEndpoint, {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        model: "local-model",
                        messages: [
                            {
                                role: "system",
                                content: "Bạn là trợ lý AI. Hãy tổng hợp dữ liệu sau và trả lời bằng tiếng Việt tự nhiên, ngắn gọn."
                            },
                            {
                                role: "user",
                                content: userQuery
                            },
                            {
                                role: "assistant",
                                content: `Đã gọi function ${functionCall.function}`
                            },
                            {
                                role: "user",
                                content: `Kết quả: ${JSON.stringify(functionResult)}`
                            }
                        ],
                        temperature: 0.7,
                        max_tokens: 300,
                        stream: false
                    })
                });

                if (finalResponse.ok) {
                    const finalData = await finalResponse.json();
                    console.log('📥 Final LM response:', finalData);
                    return finalData.choices[0].message.content;
                }
            }
        } catch (e) {
            console.log('ℹ️ Not a function call, using direct response');
        }

        // Direct text response
        return responseText;
    }

    async executeFunction(name, args) {
        const baseUrl = window.location.origin;
        
        console.log(`📞 Executing function: ${name}`, args);

        switch(name) {
            // ===== DATA FUNCTIONS =====
            case 'get_workers':
                return await this.apiCall(`${baseUrl}/api/workers`);
            
            case 'get_recent_alerts':
                const limit = args.limit || 10;
                return await this.apiCall(`${baseUrl}/api/dashboard/alerts/recent?limit=${limit}`);
            
            case 'get_helmet_status':
                const macAddress = args.mac_address;
                if (!macAddress) {
                    return { error: 'MAC address required' };
                }
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
            
            case 'read_dashboard_stats':
                return await this.readDashboardStats();
            
            // ===== UI CONTROL FUNCTIONS (non-navigation) =====
            case 'highlight_element':
                const selector = args.selector;
                const message = args.message || '';
                if (!selector) {
                    return { error: 'Selector required' };
                }
                return this.highlightElement(selector, message);
            
            case 'scroll_to_element':
                const scrollSelector = args.selector;
                if (!scrollSelector) {
                    return { error: 'Selector required' };
                }
                return this.scrollToElement(scrollSelector);
            
            // ===== SOUND EFFECTS =====
            case 'play_electric_shock':
                return this.playElectricShock();
            
            default:
                return { error: 'Unknown function: ' + name };
        }
    }

    highlightElement(selector, message) {
        try {
            const element = document.querySelector(selector);
            if (!element) {
                return { error: `Element not found: ${selector}` };
            }

            // Store original styles
            const originalBorder = element.style.border;
            const originalOutline = element.style.outline;
            const originalBg = element.style.backgroundColor;

            // Add highlight animation with bold border
            element.style.transition = 'all 0.5s ease';
            element.style.border = '4px solid #667eea';
            element.style.outline = '2px solid #f5576c';
            element.style.outlineOffset = '4px';
            element.style.backgroundColor = 'rgba(102, 126, 234, 0.1)';
            element.style.transform = 'scale(1.02)';
            element.style.zIndex = '9998';

            // Show message if provided
            if (message) {
                const msgDiv = document.createElement('div');
                msgDiv.innerHTML = message;
                msgDiv.style.cssText = `
                    position: absolute;
                    top: -40px;
                    left: 50%;
                    transform: translateX(-50%);
                    background: #667eea;
                    color: white;
                    padding: 8px 16px;
                    border-radius: 8px;
                    font-size: 14px;
                    font-weight: bold;
                    white-space: nowrap;
                    z-index: 9999;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                `;
                element.style.position = 'relative';
                element.appendChild(msgDiv);

                // Remove message after 3 seconds
                setTimeout(() => msgDiv.remove(), 3000);
            }

            // Remove highlight after 5 seconds
            setTimeout(() => {
                element.style.border = originalBorder;
                element.style.outline = originalOutline;
                element.style.backgroundColor = originalBg;
                element.style.transform = '';
                element.style.zIndex = '';
            }, 5000);

            return { success: true, message: 'Element highlighted' };
        } catch (error) {
            return { error: error.message };
        }
    }

    scrollToElement(selector) {
        try {
            const element = document.querySelector(selector);
            if (!element) {
                return { error: `Element not found: ${selector}` };
            }

            element.scrollIntoView({ 
                behavior: 'smooth', 
                block: 'center' 
            });

            return { success: true, message: 'Scrolled to element' };
        } catch (error) {
            return { error: error.message };
        }
    }

    playElectricShock() {
        try {
            const audio = new Audio('/sounds/electric-shock.mp3');
            audio.volume = 0.7;
            audio.play();
            return { success: true, message: '⚡ BZZZZT! ⚡' };
        } catch (error) {
            console.error('❌ Sound playback error:', error);
            return { error: 'Không phát được âm thanh' };
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

    async readDashboardStats() {
        try {
            // Read stat values from dashboard
            const totalWorkers = document.getElementById('stat-total-workers')?.textContent || '0';
            const activeWorkers = document.getElementById('stat-active-workers')?.textContent || '0';
            const alerts = document.getElementById('stat-alerts')?.textContent || '0';
            const efficiency = document.getElementById('stat-efficiency')?.textContent || '0%';

            // Build response text
            const statsText = `Tổng số công nhân: ${totalWorkers}. Đang làm việc: ${activeWorkers}. Cảnh báo hôm nay: ${alerts}. Hiệu suất: ${efficiency}.`;

            // Highlight each stat card with animation and delay
            const stats = [
                { selector: '#stat-total-workers', message: `${totalWorkers} công nhân`, delay: 0 },
                { selector: '#stat-active-workers', message: `${activeWorkers} online`, delay: 2000 },
                { selector: '#stat-alerts', message: `${alerts} cảnh báo`, delay: 4000 },
                { selector: '#stat-efficiency', message: `${efficiency} hiệu suất`, delay: 6000 }
            ];

            // Schedule highlights with delays
            stats.forEach(stat => {
                setTimeout(() => {
                    const element = document.querySelector(stat.selector);
                    if (element) {
                        // Get parent stat-card
                        const statCard = element.closest('.stat-card');
                        if (statCard) {
                            this.highlightElementWithPulse(statCard, stat.message);
                        }
                    }
                }, stat.delay);
            });

            return { 
                success: true, 
                stats: { totalWorkers, activeWorkers, alerts, efficiency },
                message: statsText
            };
        } catch (error) {
            return { error: error.message };
        }
    }

    highlightElementWithPulse(element, message) {
        // Store original styles
        const originalBorder = element.style.border;
        const originalBg = element.style.backgroundColor;
        const originalTransform = element.style.transform;

        // Add pulsing animation
        element.style.transition = 'all 0.3s ease';
        element.style.border = '4px solid #667eea';
        element.style.backgroundColor = 'rgba(102, 126, 234, 0.15)';
        element.style.transform = 'scale(1.05)';
        element.style.zIndex = '9998';

        // Add blinking effect
        let blinkCount = 0;
        const blinkInterval = setInterval(() => {
            if (blinkCount % 2 === 0) {
                element.style.boxShadow = '0 0 30px 10px rgba(102, 126, 234, 0.8)';
            } else {
                element.style.boxShadow = '0 0 10px 2px rgba(102, 126, 234, 0.4)';
            }
            blinkCount++;
            if (blinkCount >= 6) {
                clearInterval(blinkInterval);
            }
        }, 300);

        // Show message popup
        if (message) {
            const msgDiv = document.createElement('div');
            msgDiv.textContent = message;
            msgDiv.style.cssText = `
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 12px 24px;
                border-radius: 12px;
                font-size: 18px;
                font-weight: bold;
                z-index: 9999;
                box-shadow: 0 8px 24px rgba(0,0,0,0.4);
                animation: popupBounce 0.5s ease;
            `;
            
            // Add animation keyframes
            if (!document.getElementById('popup-animation-style')) {
                const style = document.createElement('style');
                style.id = 'popup-animation-style';
                style.textContent = `
                    @keyframes popupBounce {
                        0% { transform: translate(-50%, -50%) scale(0); }
                        50% { transform: translate(-50%, -50%) scale(1.1); }
                        100% { transform: translate(-50%, -50%) scale(1); }
                    }
                `;
                document.head.appendChild(style);
            }

            element.style.position = 'relative';
            element.appendChild(msgDiv);

            // Remove message after 1.5 seconds
            setTimeout(() => msgDiv.remove(), 1500);
        }

        // Remove highlight after 2 seconds
        setTimeout(() => {
            element.style.border = originalBorder;
            element.style.backgroundColor = originalBg;
            element.style.transform = originalTransform;
            element.style.boxShadow = '';
            element.style.zIndex = '';
        }, 2000);
    }

    executeNavigation(name, args) {
        const baseUrl = window.location.origin;
        console.log(`🧭 Navigating via: ${name}`);

        switch(name) {
            case 'navigate_to_dashboard':
                window.location.href = `${baseUrl}/index.html`;
                break;
            
            case 'navigate_to_positioning':
                window.location.href = `${baseUrl}/positioning-2d.html`;
                break;
            
            case 'navigate_to_alerts':
                window.location.href = `${baseUrl}/alerts.html`;
                break;
            
            case 'navigate_to_employees':
                window.location.href = `${baseUrl}/manage-employees.html`;
                break;
            
            default:
                console.error('Unknown navigation function:', name);
        }
    }

    speak(text, isNavigationPending = false) {
        console.log('🔊 Speaking:', text, 'Navigation pending:', isNavigationPending);
        
        // Stop listening while speaking to avoid echo
        if (this.isListening) {
            console.log('⏸️ Pausing recognition while speaking');
            this.recognition.stop();
        }
        
        if (this.useElevenLabs) {
            this.speakWithElevenLabs(text);
        } else {
            this.speakWithBrowser(text);
        }
    }

    async speakWithElevenLabs(text) {
        try {
            console.log('🎤 Using ElevenLabs TTS');
            this.isSpeaking = true; // Set speaking flag
            this.updateUI('speaking');

            const response = await fetch(`https://api.elevenlabs.io/v1/text-to-speech/${this.elevenLabsVoiceId}`, {
                method: 'POST',
                headers: {
                    'Accept': 'audio/mpeg',
                    'Content-Type': 'application/json',
                    'xi-api-key': this.elevenLabsApiKey
                },
                body: JSON.stringify({
                    text: text,
                    model_id: 'eleven_flash_v2_5',
                    voice_settings: {
                        stability: 0.5,
                        similarity_boost: 0.9,
                        style: 0.0,
                        use_speaker_boost: true
                    }
                })
            });

            if (!response.ok) {
                throw new Error(`ElevenLabs API error: ${response.status}`);
            }

            const audioBlob = await response.blob();
            const audioUrl = URL.createObjectURL(audioBlob);
            const audio = new Audio(audioUrl);

            audio.onplay = () => {
                console.log('🎙️ ElevenLabs speech started');
            };

            audio.onended = () => {
                console.log('✅ ElevenLabs speech ended');
                this.isSpeaking = false; // Clear speaking flag
                this.updateUI('ready');
                URL.revokeObjectURL(audioUrl);
                
                // Resume listening if always listening mode is enabled
                if (this.alwaysListening) {
                    setTimeout(() => {
                        this.startContinuousListening();
                    }, 500);
                }
            };

            audio.onerror = (error) => {
                console.error('❌ Audio playback error:', error);
                this.isSpeaking = false; // Clear speaking flag on error
                this.updateUI('ready');
                
                // Resume listening even on error
                if (this.alwaysListening) {
                    setTimeout(() => {
                        this.startContinuousListening();
                    }, 500);
                }
                
                // Fallback to browser TTS
                this.speakWithBrowser(text);
            };

            await audio.play();
        } catch (error) {
            console.error('❌ ElevenLabs TTS error:', error);
            // Fallback to browser TTS
            this.speakWithBrowser(text);
        }
    }

    speakWithBrowser(text) {
        console.log('🔊 Using browser TTS (fallback)');
        
        this.isSpeaking = true; // Set speaking flag
        
        // Cancel any ongoing speech
        this.synthesis.cancel();

        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'vi-VN';
        utterance.rate = 1.0; // Slightly slower for Vietnamese
        utterance.pitch = 1.0;
        utterance.volume = 1.0;

        // Use cached Vietnamese voice
        if (this.vietnameseVoice) {
            utterance.voice = this.vietnameseVoice;
            console.log('🎤 Using voice:', this.vietnameseVoice.name);
        } else {
            console.warn('⚠️ No Vietnamese voice available, using default');
        }

        utterance.onstart = () => {
            console.log('🎙️ Speech started');
            this.updateUI('speaking');
        };

        utterance.onend = () => {
            console.log('✅ Speech ended');
            this.isSpeaking = false; // Clear speaking flag
            this.updateUI('ready');
            
            // Resume listening if always listening mode is enabled
            if (this.alwaysListening) {
                setTimeout(() => {
                    this.startContinuousListening();
                }, 500);
            }
        };

        utterance.onerror = (event) => {
            console.error('❌ Speech error:', event);
            this.isSpeaking = false; // Clear speaking flag on error
            
            // Resume listening even on error
            if (this.alwaysListening) {
                setTimeout(() => {
                    this.startContinuousListening();
                }, 500);
            }
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
