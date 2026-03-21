#!/bin/bash

# 🚀 OVHCLOUD DEPLOYMENT SETUP SCRIPT
# Complete setup for OVHCloud VPS/Dedicated Server

echo "🚀 OVHCloud Nexural Platform Setup"
echo "==================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
   echo "⚠️  Please run as root (use sudo)"
   exit 1
fi

# Variables
APP_DIR="/var/www/nexural"
DB_NAME="nexural_prod"
DB_USER="nexural_user"
NODE_VERSION="20"

echo "📦 Step 1: System Update"
apt update && apt upgrade -y

echo "📦 Step 2: Install Dependencies"
# Install essential packages
apt install -y curl git nginx postgresql postgresql-contrib redis-server \
    build-essential python3 certbot python3-certbot-nginx ufw fail2ban

# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | bash -
apt install -y nodejs

# Install PM2 globally
npm install -g pm2

echo "🔒 Step 3: Security Setup"
# Configure firewall
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 3000/tcp
ufw --force enable

# Configure fail2ban
systemctl enable fail2ban
systemctl start fail2ban

echo "🗄️ Step 4: PostgreSQL Setup"
# Generate secure password
DB_PASSWORD=$(openssl rand -base64 16)

# Create database and user
sudo -u postgres psql << EOF
CREATE USER $DB_USER WITH ENCRYPTED PASSWORD '$DB_PASSWORD';
CREATE DATABASE $DB_NAME OWNER $DB_USER;
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;
ALTER USER $DB_USER CREATEDB;
EOF

echo "✅ Database created:"
echo "   Name: $DB_NAME"
echo "   User: $DB_USER"
echo "   Password: $DB_PASSWORD"
echo ""
echo "⚠️  Save this password securely!"

echo "📁 Step 5: Application Setup"
# Create application directory
mkdir -p $APP_DIR
cd $APP_DIR

# Clone repository (replace with your repo)
echo "Enter your Git repository URL:"
read GIT_REPO
git clone $GIT_REPO .

# Install dependencies
npm install --production

# Create environment file
cat > .env.production << EOF
# Database
DATABASE_URL=postgresql://$DB_USER:$DB_PASSWORD@localhost:5432/$DB_NAME

# Redis
REDIS_URL=redis://localhost:6379

# App
NODE_ENV=production
PORT=3000

# Add your other environment variables here
EOF

echo "🔧 Step 6: Build Application"
npm run build

echo "🌐 Step 7: Nginx Configuration"
# Get domain name
echo "Enter your domain name (e.g., example.com):"
read DOMAIN_NAME

# Create Nginx config
cat > /etc/nginx/sites-available/nexural << EOF
server {
    listen 80;
    listen [::]:80;
    server_name $DOMAIN_NAME www.$DOMAIN_NAME;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Proxy settings
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_cache_bypass \$http_upgrade;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # Static files
    location /_next/static {
        alias $APP_DIR/.next/static;
        expires 365d;
        add_header Cache-Control "public, immutable";
    }

    location /public {
        alias $APP_DIR/public;
        expires 30d;
        add_header Cache-Control "public";
    }
}
EOF

# Enable site
ln -sf /etc/nginx/sites-available/nexural /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx

echo "🚀 Step 8: PM2 Setup"
# Create PM2 ecosystem file
cat > $APP_DIR/ecosystem.config.js << 'EOF'
module.exports = {
  apps: [{
    name: 'nexural',
    script: 'npm',
    args: 'start',
    cwd: '/var/www/nexural',
    instances: 'max',
    exec_mode: 'cluster',
    autorestart: true,
    watch: false,
    max_memory_restart: '1G',
    env: {
      NODE_ENV: 'production',
      PORT: 3000
    },
    error_file: '/var/log/pm2/nexural-error.log',
    out_file: '/var/log/pm2/nexural-out.log',
    log_file: '/var/log/pm2/nexural-combined.log',
    time: true
  }]
};
EOF

# Start application with PM2
pm2 start ecosystem.config.js
pm2 save
pm2 startup systemd -u root --hp /root

echo "🔐 Step 9: SSL Certificate"
echo "Do you want to setup SSL now? (y/n)"
read SETUP_SSL

if [ "$SETUP_SSL" = "y" ]; then
    echo "Enter your email for SSL certificate:"
    read SSL_EMAIL
    certbot --nginx -d $DOMAIN_NAME -d www.$DOMAIN_NAME --non-interactive --agree-tos -m $SSL_EMAIL
    
    # Auto-renewal
    echo "0 0 * * 0 certbot renew --quiet" | crontab -
fi

echo "📊 Step 10: Monitoring Setup"
# Create health check endpoint
cat > $APP_DIR/scripts/health-check.sh << 'EOF'
#!/bin/bash
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health-check)
if [ $response -eq 200 ]; then
    echo "✅ Health check passed"
else
    echo "❌ Health check failed"
    pm2 restart nexural
fi
EOF

chmod +x $APP_DIR/scripts/health-check.sh

# Add to crontab for monitoring
(crontab -l 2>/dev/null; echo "*/5 * * * * $APP_DIR/scripts/health-check.sh") | crontab -

echo "🎯 Step 11: Backup Setup"
# Create backup script
cat > $APP_DIR/scripts/backup.sh << EOF
#!/bin/bash
BACKUP_DIR="/backups/nexural"
DATE=\$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p \$BACKUP_DIR

# Backup database
pg_dump -U $DB_USER -h localhost $DB_NAME > \$BACKUP_DIR/db_\$DATE.sql

# Backup uploads
tar -czf \$BACKUP_DIR/uploads_\$DATE.tar.gz $APP_DIR/public/uploads

# Keep only last 7 days of backups
find \$BACKUP_DIR -type f -mtime +7 -delete

echo "✅ Backup completed: \$DATE"
EOF

chmod +x $APP_DIR/scripts/backup.sh

# Add daily backup to crontab
(crontab -l 2>/dev/null; echo "0 2 * * * $APP_DIR/scripts/backup.sh") | crontab -

echo ""
echo "===================================="
echo "🎉 OVHCloud Setup Complete!"
echo "===================================="
echo ""
echo "📋 Important Information:"
echo "------------------------"
echo "Database Name: $DB_NAME"
echo "Database User: $DB_USER"
echo "Database Password: $DB_PASSWORD"
echo "Application Directory: $APP_DIR"
echo ""
echo "📊 Services Status:"
pm2 status
echo ""
echo "🔍 Next Steps:"
echo "1. Update .env.production with your API keys"
echo "2. Run database migrations: npm run migrate"
echo "3. Set up your domain DNS to point to this server"
echo "4. Access your site at: http://$DOMAIN_NAME"
echo ""
echo "📝 Useful Commands:"
echo "- View logs: pm2 logs nexural"
echo "- Restart app: pm2 restart nexural"
echo "- Monitor: pm2 monit"
echo "- Nginx logs: tail -f /var/log/nginx/access.log"
echo ""
echo "✅ Your platform is now running on OVHCloud!"
