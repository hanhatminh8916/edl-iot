#!/usr/bin/env python3
"""
🧪 GEMINI API TEST SUITE - Voice Assistant
Test các models để tìm model tốt nhất cho Voice Assistant
"""

import google.generativeai as genai
import json
import sys

# API Configuration
API_KEY = "AIzaSyCt5n2JKmdKopGRT5og1cHrfRp9bmP1k_E"
genai.configure(api_key=API_KEY)

# Test với các models khác nhau
MODELS_TO_TEST = [
    "gemini-2.5-flash",      # Mới nhất
    "gemini-1.5-flash",      # Stable
    "gemini-1.5-pro",        # Pro version
    "gemini-2.0-flash-exp"   # Experimental (sẽ fail)
]

def test_model(model_name):
    """Test một model cụ thể"""
    print(f"\n{'='*60}")
    print(f"Testing model: {model_name}")
    print(f"{'='*60}")
    
    try:
        model = genai.GenerativeModel(model_name)
        response = model.generate_content("Có bao nhiêu công nhân đang online?")
        
        print(f"✅ SUCCESS!")
        print(f"Response: {response.text[:100]}...")
        return True
        
    except Exception as e:
        error_msg = str(e)
        print(f"❌ FAILED: {error_msg}")
        
        # Check if it's quota error
        if "quota" in error_msg.lower() or "429" in error_msg:
            print(f"   → Quota exceeded (free tier không hỗ trợ model này)")
        elif "not found" in error_msg.lower() or "404" in error_msg:
            print(f"   → Model không tồn tại")
        
        return False

def test_function_calling(model_name):
    """Test function calling - giống voice assistant"""
    print(f"\n{'='*60}")
    print(f"Testing FUNCTION CALLING with: {model_name}")
    print(f"{'='*60}")
    
    try:
        # Define tools (functions) giống voice-assistant.js
        get_workers_func = genai.protos.FunctionDeclaration(
            name="get_workers",
            description="Lấy danh sách công nhân đang làm việc",
            parameters=genai.protos.Schema(
                type=genai.protos.Type.OBJECT,
                properties={}
            )
        )
        
        model = genai.GenerativeModel(
            model_name,
            tools=[get_workers_func]
        )
        
        response = model.generate_content("Có bao nhiêu công nhân?")
        
        print(f"✅ FUNCTION CALLING SUCCESS!")
        
        # Check if function was called
        if hasattr(response, 'candidates') and response.candidates:
            for part in response.candidates[0].content.parts:
                if hasattr(part, 'function_call'):
                    print(f"🔧 Function called: {part.function_call.name}")
                    return True
        
        print(f"📝 Got text response instead: {response.text[:100]}...")
        return True
        
    except Exception as e:
        print(f"❌ FUNCTION CALLING FAILED: {str(e)}")
        return False

def main():
    print("\n" + "="*60)
    print("🧪 GEMINI API TEST SUITE - Voice Assistant")
    print("="*60)
    print(f"API Key: {API_KEY[:20]}...{API_KEY[-10:]}")
    
    results = {}
    
    # Test 1: Basic text generation
    print("\n📝 TEST 1: Basic Text Generation")
    for model in MODELS_TO_TEST:
        results[model] = test_model(model)
    
    # Test 2: Function calling (chỉ test model thành công)
    print("\n🔧 TEST 2: Function Calling")
    successful_models = [m for m, success in results.items() if success]
    
    if successful_models:
        # Chỉ test model đầu tiên để tiết kiệm quota
        test_function_calling(successful_models[0])
    
    # Summary
    print("\n" + "="*60)
    print("📊 TEST SUMMARY")
    print("="*60)
    
    for model, success in results.items():
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{model:30} {status}")
    
    # Recommendation
    print("\n💡 RECOMMENDATION:")
    best_model = None
    for model in ["gemini-2.5-flash", "gemini-1.5-flash", "gemini-1.5-pro"]:
        if results.get(model):
            best_model = model
            break
    
    if best_model:
        print(f"   ✅ Use model: {best_model}")
        print(f"\n   📝 Update VoiceAssistantController.java:")
        print(f'   String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/{best_model}:generateContent";')
        
        print(f"\n   📝 Current deployment URL should be:")
        print(f"   https://generativelanguage.googleapis.com/v1beta/models/{best_model}:generateContent")
    else:
        print("   ⚠️ No working model found! Check API key and quota.")
        sys.exit(1)

if __name__ == "__main__":
    main()
