# 🧠 InsightBase AI

**InsightBase AI** is a complete, professional-grade intelligent knowledge management desktop application built with **JavaFX 23**, **LangChain4j**, and **OpenAI & Hugging Face AI models**. It enables users to chat, search, and retrieve contextual answers from custom document collections using **Retrieval-Augmented Generation (RAG)** technology.

---

## 🚀 Features

### ✅ Implemented Core Functionalities
- 💬 **AI Chat Interface** — Real-time conversational AI using LangChain4j + OpenAI GPT & Hugging Face models
- 📂 **Advanced Document Management** — Upload, process, and manage TXT, PDF, DOCX, MD, Java, XML, JSON, YAML files
- 🔍 **Intelligent Search** — Full-text search with advanced filtering and export capabilities
- ⚙️ **Comprehensive Settings Panel** — Complete API configuration, preferences, and system management
- 📊 **Statistics & Monitoring** — Real-time analytics, performance tracking, and system information
- 🎨 **Professional UI Design** — Modern, responsive interface with comprehensive CSS styling
- �️ **Robust Error Handling** — User-friendly error messages and graceful failure recovery
- 💾 **Persistent Configuration** — Automatic settings management and window state preservation

### 🔧 Advanced Features
- 🔄 **Document Processing Pipeline** — Text extraction, chunking, and validation with detailed feedback
- 🌐 **Multi-format Support** — Extensible architecture for various document formats
- 📈 **Performance Optimization** — Efficient processing with progress tracking and memory management
- � **Input Validation** — Comprehensive validation for all user inputs and configurations
- 📝 **Comprehensive Logging** — Detailed application logging with performance metrics

---

## 🏗️ Project Architecture (MVC Pattern)

### **Complete Implementation Structure**
```
InsightBaseAI/
├── src/main/java/com/example/insightbaseai/
│   ├── MainApp.java                     → Application entry point
│   ├── controller/
│   │   ├── MainController.java          → Navigation & main window management
│   │   ├── ChatController.java          → AI chat interface controller
│   │   ├── AdminController.java         → Document management controller
│   │   ├── SearchController.java        → Advanced search functionality
│   │   └── SettingsController.java      → Configuration management
│   ├── model/
│   │   ├── ChatMessage.java             → Chat data structure with validation
│   │   ├── DocumentEntry.java           → Document metadata & statistics
│   │   └── KnowledgeBase.java           → Knowledge management with RAG support
│   ├── service/
│   │   └── AIService.java               → Complete RAG pipeline with LangChain4j
│   └── util/
│       ├── FileUtils.java               → Multi-format document processing
│       ├── LoggerUtil.java              → Comprehensive logging system
│       ├── ValidationUtil.java          → Input validation & sanitization
│       ├── ErrorHandler.java            → Error management & user dialogs
│       └── ConfigurationManager.java    → Settings persistence & management
├── src/main/resources/
│   ├── fxml/
│   │   ├── main_view.fxml              → Main application window
│   │   ├── chat_view.fxml              → Chat interface layout
│   │   ├── admin_view.fxml             → Document management UI
│   │   ├── search_view.fxml            → Search interface
│   │   └── settings_view.fxml          → Configuration panel
│   └── styles/
│       ├── application.css             → Comprehensive styling
│       └── simple.css                  → Lightweight theme
└── pom.xml                             → Maven configuration with all dependencies
```

---

## 🛠️ Tech Stack

### **Core Technologies**
- **Language:** Java 21 LTS (Latest Long-Term Support)
- **Frontend:** JavaFX 23.0.1 with FXML
- **AI Framework:** LangChain4j 0.35.0
- **LLM Providers:** OpenAI (GPT-4o-mini, GPT-4o) & Hugging Face (Llama-3.1, et al.)
- **Build Tool:** Maven 3.9.11
- **Vector Storage:** In-memory embedding store with extensible architecture

### **Key Dependencies**
- **Document Processing:** Apache PDFBox, Apache POI (for PDF & DOCX)
- **JSON Processing:** Jackson Core & Databind
- **Utilities:** Apache Commons IO
- **Logging:** Apache Log4j2
- **Testing:** JUnit 5, Mockito
- **UI Styling:** Modern CSS with comprehensive theming  

---

## 📦 Setup & Installation

### **Prerequisites**
- ☑️ **Java 21 LTS** or higher
- ☑️ **Maven 3.6+** 
- ☑️ **Internet connection** (for dependencies & API calls)
- ☑️ **API Key** (OpenAI and/or Hugging Face)

### **Quick Start Guide**

1. **Clone or Download the Project**
   ```bash
   git clone https://github.com/yourusername/insightbaseai.git
   cd InsightBaseAI
   ```

2. **Verify Java Version**
   ```bash
   java -version
   # Should show Java 21 or higher
   ```

3. **Build the Application**
   ```bash
   mvn clean compile
   ```

4. **Run InsightBase AI**
   ```bash
   mvn javafx:run
   ```

5. **Initial Configuration**
   - Open the **⚙️ Settings** tab
   - Choose your preferred provider (**OpenAI** or **Hugging Face**)
   - Enter your API key and select a model
   - Configure preferences and save settings
   - You're ready to go! 🚀

### **Alternative Run Methods**
```bash
# Run without clean
mvn javafx:run

# Run with full rebuild
mvn clean javafx:run

# Build and run separately
mvn compile && mvn javafx:run
```  

---

## 🎯 Application Usage

### **Getting Started Workflow**

1. **⚙️ Settings Configuration**
   - Choose AI Provider: **OpenAI** or **Hugging Face**
   - Configure corresponding API key and model preferences
   - Adjust RAG parameters (chunk size, retrieval results)
   - Set application preferences and theme options

2. **📋 Document Management (Admin)**
   - Upload documents (TXT, PDF, DOCX, MD, Java, XML, JSON, YAML)
   - View document statistics and processing status
   - Manage knowledge base collections

3. **💬 AI-Powered Chat**
   - Ask questions about your uploaded documents
   - Get contextually relevant answers using RAG
   - View conversation history and manage chat sessions

4. **🔍 Advanced Search**
   - Search across all documents with filters
   - Export search results and findings
   - Navigate to specific document sections

### **Key Features Breakdown**

| Feature | Description | Status |
|---------|-------------|--------|
| 🚀 **Modern UI** | Professional JavaFX interface with CSS styling | ✅ Complete |
| 🧠 **AI Integration** | LangChain4j + OpenAI & Hugging Face models | ✅ Complete |
| 📄 **Document Processing** | Multi-format support with text extraction | ✅ Complete |
| 🔍 **Intelligent Search** | Full-text search with advanced filtering | ✅ Complete |
| ⚙️ **Configuration** | Persistent settings with validation | ✅ Complete |
| 🛡️ **Error Handling** | Comprehensive error management | ✅ Complete |
| 📊 **Analytics** | Performance tracking and statistics | ✅ Complete |
| 🎨 **Professional Design** | Modern, responsive UI components | ✅ Complete |

---

## 🏗️ Architecture Highlights

### **Design Patterns**
- **MVC (Model-View-Controller)** — Clean separation of concerns
- **Singleton Pattern** — Configuration and error management
- **Observer Pattern** — UI updates and event handling
- **Strategy Pattern** — Document processing for different formats

### **Key Technical Decisions**
- **JavaFX 23** — Modern, native desktop application framework
- **LangChain4j** — Robust AI integration with multi-provider RAG support
- **In-memory Vector Store** — Fast retrieval with extensible architecture
- **Maven Build System** — Reliable dependency management
- **Comprehensive Error Handling** — User-friendly experience
- **Persistent Configuration** — Seamless user experience across sessions

---

## � Future Enhancements

### **Planned Improvements**
- 🌐 **External Vector Databases** — Integration with FAISS, Chroma, or Pinecone
- 🎨 **Theme System** — Custom theme support beyond dark/light
- 🔌 **Plugin Architecture** — Extensible system for custom document processors
- 🌍 **Multi-language Support** — Internationalization (i18n) capabilities

### **Technical Roadmap**
- **Enhanced RAG Pipeline** — Query transformation and result re-ranking
- **Distributed Processing** — Support for large document collections
- **API Integration** — REST API for external system integration
- **Cloud Deployment** — Docker containerization and cloud-native features

---

## 📊 Project Status: **100% Complete** ✅

**All core requirements successfully implemented:**
- ✅ Java 21 LTS upgrade and optimization
- ✅ Complete MVC architecture implementation
- ✅ Professional JavaFX desktop application
- ✅ AI integration with RAG capabilities
- ✅ Comprehensive document management system
- ✅ Advanced search and filtering functionality
- ✅ Modern, professional UI design
- ✅ Robust error handling and validation
- ✅ Persistent configuration management
- ✅ Complete navigation and user experience

---

## 👥 Development Team

**Project:** InsightBase AI - Intelligent Knowledge Management System  
**Course:** Advanced Programming (ITS66704)  
**Institution:** Taylor's University  
**Year:** 2024/2025  

**Technologies Mastered:**
- Java 21 LTS Development
- JavaFX Desktop Application Development
- AI/ML Integration with LangChain4j
- RAG (Retrieval-Augmented Generation) Implementation
- Professional UI/UX Design
- Maven Build System Management
- Comprehensive Error Handling
- Software Architecture & Design Patterns

---

**🎉 Ready for Production Use!**

*InsightBase AI represents a complete, professional-grade application demonstrating advanced Java programming, modern UI development, AI integration, and software engineering best practices.*  
