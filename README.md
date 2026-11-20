1.	Project Title
SINAG: A System for Information, News, Advisories, and Guides on El Niño Awareness and Preparedness with a Quiz Game
2. Project Description
SINAG is a Java based application designed as a centralized repository of reliable information regarding El Niño in the Philippines. This provides users with access to educational articles, advisories, frequently asked questions (FAQs), and emergency preparedness guidelines. In addition, the system contains administrative tools for content management and features an interactive, assessment module with automated scoring and feedback to evaluate user comprehension.
3. Problem Being Addressed
During El Niño, information about drought, heat, water shortage, and safety measures is often scattered across many websites, social media posts, and announcements. Some of it is outdated, incomplete, or false. As a result, many people do not know how to prepare or where to find trustworthy guidance.
SINAG addresses this by:
•	Gathering El Niño information in one organized system
•	Labeling each entry with its source and date posted so users can judge reliability
•	Making information easy to find through search and category filtering
•	Encouraging learning through a quiz
4.	Project Objectives
General objective: To develop a console-based information system that improves public awareness and preparedness for El Niño.
Specific objectives:
•	To provide educational articles, advisories, FAQs, and preparedness guides in one system.
•	To allow an administrator to add, edit, and delete content.
•	To let users search entries by keyword and filter them by category.
•	To provide a quiz with scoring and feedback.
5. Target Users
General Public - Browse educational articles, read advisories, view FAQs and preparedness guides, search or filter information, and take quizzes to check their understanding.
Administrator - Manage educational articles, advisories, FAQs, preparedness guides, announcements, quiz questions, answer choices, correct answers, and explanations.
6.	Main Features
•	Educational Articles: Articles about El Niño causes, effects, and impacts.
•	Public Advisories: View advisories with severity level with announcements.
•	FAQs: Quick answers to common questions.
•	Category Filtering: Show only one type of content at a time.
•	Admin Panel: Password-protected add, edit, and delete of entries.
•	Search, Categories, and Admin Management: Allow users to search and filter information by topic. Provide authorized administrators with tools to manage content, announcements, and quiz questions.
•	Interactive Quiz Game: Single-player quiz with score, correct or wrong feedback, the right answer, and a short explanation.
7. Technologies Used
•	Java (JDK 17) - Main programming language
•	Git and GitHub - Version control
•	java.util (ArrayList, Scanner, Random) - Data storage, input, quiz randomization
8. Team Members and Roles
1.	Cabrera, Dwyane Lloyd D. - GUI and user-module lead, help design the Java GUI, main menu, navigation, and public user functions.
2.	Ramirez, Fatima C. -  help organize educational content and advisories and implement database storage, Readme and paper making.
3.	Villanueva, Adrian Justine M. - Quiz and integration lead, help implement quiz mechanics, scoring and feedback, integrate modules, and coordinate testing.
9.	OOP Concepts Demonstrated
•	Classes and Objects: Create classes such as User, Administrator, Article, Advisory, QuizQuestion, and QuizGame. Objects represent individual users, posts, advisories, and quiz activities.
•	Methods: Use methods to perform actions such as displayContent(), searchContent(), publishAdvisory(), checkAnswer(), calculateScore(), and manageQuestions().
•	Encapsulation: Keep data fields private and provide appropriate public methods (getters/setters) to control access to user, content, and quiz information.
•	Inheritance: Use a general Content class for shared properties and let specific content classes such as Article, Advisory, FAQ, and PreparednessGuide inherit common fields and behavior.
•	Polymorphism:  Use method overriding where different content types need their own display behavior, while a common reference can handle different content objects.
•	Abstraction: Use abstract classes or interfaces for shared content-management behavior where appropriate, so specific content classes provide their own implementations.
•	Exception Handling: Handle possible errors such as invalid input, missing required fields, database connection problems, and unavailable records using try-catch and suitable validation.
•	Arrays/Data Handling: Use arrays or other suitable collections to store and process quiz choices, questions, search results, and lists of information retrieved from the database.
10.	Instructions for Running the Program
11.	Sample Screenshots
12.	Known limitations
• The system will not independently generate live weather forecasts or government warnings unless an official data integration is separately implemented.
• Advisory accuracy and timeliness will depend on information entered and verified by authorized administrators.
• The application will not replace official announcements, weather bulletins, emergency services, or guidance from government agencies and local authorities.
• The quiz is for educational purposes only and will not certify a user's emergency-response capability.
• The project focuses on the features listed in this proposal; other advanced features or external integrations are outside the current scope.
13. Future Improvements
•	Live data from PAGASA or other official sources
•	Push or SMS notifications for critical advisories
•	Multiplayer quiz and leaderboard
•	Database storage (MySQL or SQLite)
14.	Project Contributors
1.	Cabrera, Dwyane Lloyd D.
2.	Ramirez, Fatima C.
3.	Villanueva, Adrian Justine M.
