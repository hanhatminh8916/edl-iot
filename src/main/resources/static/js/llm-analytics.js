/**
 * LLM Analytics Integration - Frontend Client
 * 
 * Sử dụng API analytics để thêm khả năng AI vào dashboard
 */

const LLM_API_BASE = '/api/analytics';

/**
 * Natural Language Query
 * Hỏi dữ liệu bằng tiếng Việt hoặc English
 * 
 * @param {string} query - Câu hỏi
 * @returns {Promise<object>} Response từ LLM
 */
async function askQuestion(query) {
    console.log('🤖 Asking LLM:', query);
    
    try {
        const response = await fetch(`${LLM_API_BASE}/query`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                query: query,
                executeQueries: true,
                includeData: true
            })
        });

        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();
        console.log('✅ LLM response:', data);
        
        return data;
    } catch (error) {
        console.error('❌ Failed to ask question:', error);
        throw error;
    }
}

/**
 * Auto-generate Insights
 * Tự động tạo insights từ dữ liệu
 * 
 * @param {string} timeRange - "7d", "30d", "90d"
 * @param {string} department - Phòng ban (optional)
 * @returns {Promise<object>} Insights và recommendations
 */
async function generateInsights(timeRange = '30d', department = null) {
    console.log('📊 Generating insights...');
    
    try {
        const response = await fetch(`${LLM_API_BASE}/insights`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                timeRange: timeRange,
                department: department
            })
        });

        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();
        console.log('✅ Insights generated:', data);
        
        return data;
    } catch (error) {
        console.error('❌ Failed to generate insights:', error);
        throw error;
    }
}

/**
 * Root Cause Analysis
 * Phân tích nguyên nhân gốc rễ của một alert
 * 
 * @param {number} alertId - ID của alert
 * @returns {Promise<object>} Analysis kết quả
 */
async function analyzeRootCause(alertId) {
    console.log('🔍 Analyzing root cause for alert', alertId);
    
    try {
        const response = await fetch(`${LLM_API_BASE}/root-cause/${alertId}?includeContext=true`);

        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();
        console.log('✅ Root cause analysis:', data);
        
        return data;
    } catch (error) {
        console.error('❌ Failed to analyze root cause:', error);
        throw error;
    }
}

/**
 * Risk Prediction
 * Dự đoán rủi ro cho một công nhân
 * 
 * @param {number} workerId - ID công nhân
 * @param {number} horizonDays - Số ngày dự đoán (default: 7)
 * @returns {Promise<object>} Prediction kết quả
 */
async function predictWorkerRisk(workerId, horizonDays = 7) {
    console.log('⚠️ Predicting risk for worker', workerId);
    
    try {
        const response = await fetch(`${LLM_API_BASE}/risk-prediction/${workerId}?horizonDays=${horizonDays}`);

        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();
        console.log('✅ Risk prediction:', data);
        
        return data;
    } catch (error) {
        console.error('❌ Failed to predict risk:', error);
        throw error;
    }
}

/**
 * Generate Report
 * Tạo báo cáo tự động
 * 
 * @param {string} reportType - "weekly", "monthly", "quarterly"
 * @param {string} timeRange - "7d", "30d", "90d"
 * @param {string} audience - "management", "technical", "regulatory"
 * @returns {Promise<object>} Report markdown và data
 */
async function generateReport(reportType = 'weekly', timeRange = '7d', audience = 'management') {
    console.log('📄 Generating report...');
    
    try {
        const response = await fetch(`${LLM_API_BASE}/report`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                reportType: reportType,
                timeRange: timeRange,
                audience: audience
            })
        });

        if (!response.ok) {
            throw new Error(`API error: ${response.status}`);
        }

        const data = await response.json();
        console.log('✅ Report generated:', data);
        
        return data;
    } catch (error) {
        console.error('❌ Failed to generate report:', error);
        throw error;
    }
}

/**
 * Display LLM Response in UI
 * Helper function để hiển thị response từ LLM
 */
function displayLlmResponse(response, containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Clear container
    container.innerHTML = '';

    // Natural Language Response
    if (response.natural_language_response) {
        const responseDiv = document.createElement('div');
        responseDiv.className = 'llm-response';
        responseDiv.innerHTML = `
            <h4>📝 Kết quả:</h4>
            <p>${response.natural_language_response}</p>
        `;
        container.appendChild(responseDiv);
    }

    // Generated Report (markdown)
    if (response.report_markdown) {
        const reportDiv = document.createElement('div');
        reportDiv.className = 'llm-report';
        reportDiv.innerHTML = `
            <h4>📄 Báo cáo:</h4>
            <div id="llm-report-rendered"></div>
        `;
        container.appendChild(reportDiv);

        // If 'marked' is available (recommended), use it to convert markdown to HTML
        // and sanitize via simple approach.
        const md = response.report_markdown;
        try {
            let html = null;

            if (typeof window.marked === 'function') {
                html = window.marked.parse(md);
            } else {
                // Lightweight markdown -> HTML converter for basic formatting
                function escapeHtml(s) {
                    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                }

                function simpleMarkdownToHtml(markdown) {
                    const lines = markdown.split(/\r?\n/);
                    let out = '';
                    let inList = false;
                    let inCode = false;
                    let codeBuffer = [];

                    for (let i = 0; i < lines.length; i++) {
                        let line = lines[i];
                        if (line.startsWith('```')) {
                            if (!inCode) { inCode = true; codeBuffer = []; continue; }
                            // close code
                            inCode = false;
                            out += '<pre><code>' + escapeHtml(codeBuffer.join('\n')) + '</code></pre>';
                            continue;
                        }
                        if (inCode) { codeBuffer.push(line); continue; }

                        if (/^\s*-\s+/.test(line)) {
                            if (!inList) { inList = true; out += '<ul>'; }
                            const item = line.replace(/^\s*-\s+/, '');
                            out += '<li>' + item.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/\*(.*?)\*/g, '<em>$1</em>') + '</li>';
                            // if next line not a list, close
                            const next = lines[i+1] || '';
                            if (!/^\s*-\s+/.test(next)) { out += '</ul>'; inList = false; }
                            continue;
                        }

                        // headings
                        if (/^#\s+/.test(line)) { out += '<h1>' + line.replace(/^#\s+/, '') + '</h1>'; continue; }
                        if (/^##\s+/.test(line)) { out += '<h2>' + line.replace(/^##\s+/, '') + '</h2>'; continue; }
                        if (/^###\s+/.test(line)) { out += '<h3>' + line.replace(/^###\s+/, '') + '</h3>'; continue; }

                        if (/^---+$/.test(line.trim())) { out += '<hr/>'; continue; }

                        if (line.trim() === '') { out += '<p></p>'; continue; }

                        // inline bold/italic
                        let converted = escapeHtml(line)
                            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                            .replace(/\*(.*?)\*/g, '<em>$1</em>');

                        out += '<p>' + converted + '</p>';
                    }
                    return out;
                }

                html = simpleMarkdownToHtml(md);
            }

            // Sanitize: prefer DOMPurify if available
            let safeHtml = html;
            if (typeof window.DOMPurify !== 'undefined' && typeof window.DOMPurify.sanitize === 'function') {
                safeHtml = window.DOMPurify.sanitize(html);
            } else {
                // basic sanitization: strip <script> tags
                safeHtml = html.replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '');
            }

            document.getElementById('llm-report-rendered').innerHTML = safeHtml;
        } catch (e) {
            console.error('Failed to render report markdown', e);
            const pre = document.createElement('pre');
            pre.textContent = md;
            document.getElementById('llm-report-rendered').appendChild(pre);
        }
    }

    // Insights
    if (response.insights && response.insights.length > 0) {
        const insightsDiv = document.createElement('div');
        insightsDiv.className = 'llm-insights';
        insightsDiv.innerHTML = `
            <h4>💡 Insights:</h4>
            <ul>
                ${response.insights.map(insight => `<li>${insight}</li>`).join('')}
            </ul>
        `;
        container.appendChild(insightsDiv);
    }

    // Recommendations
    if (response.recommendations && response.recommendations.length > 0) {
        const recsDiv = document.createElement('div');
        recsDiv.className = 'llm-recommendations';
        recsDiv.innerHTML = `
            <h4>🎯 Recommendations:</h4>
            <div class="recommendations-list">
                ${response.recommendations.map(rec => `
                    <div class="recommendation ${rec.priority.toLowerCase()}">
                        <span class="priority">${rec.priority}</span>
                        <strong>${rec.action}</strong>
                        <p>${rec.impact}</p>
                    </div>
                `).join('')}
            </div>
        `;
        container.appendChild(recsDiv);
    }

    // Follow-up Questions
    if (response.follow_up_questions && response.follow_up_questions.length > 0) {
        const followUpDiv = document.createElement('div');
        followUpDiv.className = 'llm-follow-up';
        followUpDiv.innerHTML = `
            <h4>❓ Gợi ý câu hỏi tiếp theo:</h4>
            <div class="follow-up-buttons">
                ${response.follow_up_questions.map((q, i) => 
                    `<button class="btn-follow-up" onclick="askQuestion('${q}')">${q}</button>`
                ).join('')}
            </div>
        `;
        container.appendChild(followUpDiv);
    }
}

/**
 * Example Usage
 */
async function exampleUsage() {
    // 1. Hỏi câu hỏi đơn giản
    const answer1 = await askQuestion("Có bao nhiêu cảnh báo hôm nay?");
    displayLlmResponse(answer1, 'llm-output-1');

    // 2. Hỏi câu hỏi phức tạp
    const answer2 = await askQuestion("Công nhân nào có nguy cơ tai nạn cao nhất?");
    displayLlmResponse(answer2, 'llm-output-2');

    // 3. Generate insights
    const insights = await generateInsights('30d', 'Xây dựng');
    displayLlmResponse(insights, 'insights-output');

    // 4. Root cause analysis
    const rootCause = await analyzeRootCause(123);
    console.log('Root cause:', rootCause);

    // 5. Risk prediction
    const risk = await predictWorkerRisk(45, 7);
    console.log('Risk prediction:', risk);

    // 6. Generate report
    const report = await generateReport('weekly', '7d', 'management');
    console.log('Report:', report.report_markdown);
}

// Export functions
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        askQuestion,
        generateInsights,
        analyzeRootCause,
        predictWorkerRisk,
        generateReport,
        displayLlmResponse
    };
}
